package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameter, path, put}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import scala.concurrent.Future
import scala.io.StdIn
import akka.pattern.ask

object BiddingAgent {

  case class Bid(userId: String, offer: Int)
  case object GetBids
  case class Bids(bids: List[Bid])

  case class Campaign(id: Int, userId: Int, country: String, targeting: Targeting, banners: List[Banner], bid: Double)
  case class Targeting(cities: List[String], targetedSiteIds: List[Int])
  case class Banner(id: Int, src: String, width: Int, height: Int)

  case class BidRequest(id: String, imp: Option[List[Impression]], site: Site, user: Option[User], device: Option[Device])
  case class Impression(id: String, wmin: Option[Int], wmax: Option[Int], w: Option[Int], hmin: Option[Int], hmax: Option[Int], h: Option[Int], bidFloor: Option[Double])
  case class Site(id: Int, domain: String)
  case class User(id: String, geo: Option[Geo])
  case class Device(id: String, geo: Option[Geo])
  case class Geo(country: Option[String], city: Option[String], lat: Option[Double], lon: Option[Double])

  case class BidResponse(id: String, bidRequestId: String, price: Double, adid: Option[String], banner: Option[Banner])

  var bidResponseCounter:Int = 0

  private def matchCampaign(bidRequest: BidRequest, campaigns: List[Campaign]): Option[List[Campaign]] = {
    var qualifiedCampaigns = List.empty[Campaign]
    val countries:Option[List[String]] = getBidRequestCountries(bidRequest)
    val cities:Option[List[String]] = getBidRequestCities(bidRequest)

    //Match if no country in bid request
    if(countries == None){
      println("This bid request has no associated countries to qualify")
      return None
    }
    //Match if city in bid request
    if(cities == None){
      println("This bid request has no associated cities to qualify")
      return None
    }

    qualifiedCampaigns = campaigns.filter(campaign =>{
      val countryMatch = countries.get.contains(campaign.country) //Match country
      //println(s"for campaign ${campaign.id} cities: $cities, campaign cities ${campaign.targeting.cities}")
      val cityMatch = !cities.get.intersect(campaign.targeting.cities).isEmpty  //Match cities
      val siteMatch = campaign.targeting.targetedSiteIds.contains(bidRequest.site.id) //Match site id
      val impressionMatch = findMatchingBanner(campaign, bidRequest.imp).nonEmpty  //Match banner
      //println(s"Match for campaign ${campaign.id}: countryMatch=$countryMatch, cityMatch=$cityMatch, siteMatch=$siteMatch, impressionMatch=$impressionMatch")
      countryMatch && cityMatch && siteMatch && impressionMatch
    })

    if(qualifiedCampaigns.isEmpty) None else Some(qualifiedCampaigns)
  }

  private def findMatchingBanner(campaign: Campaign, imp: Option[List[Impression]]): Option[Banner]={
    val matchingBanner: Option[Banner] = campaign.banners.find(cBanner => {
      val impressions: List[Impression] = imp.getOrElse(List.empty[Impression])
      val matchingImp:Option[Impression] = impressions.find(impr => {
        val widthMatch = List(impr.w.getOrElse(0), impr.wmin.getOrElse(0), impr.wmax.getOrElse(0)).contains(cBanner.width)
        val heigthMatch = List(impr.h.getOrElse(0), impr.hmin.getOrElse(0), impr.hmax.getOrElse(0)).contains(cBanner.height)
        //println(s"bidFloor=${impr.bidFloor.getOrElse(0)}, campaign.bid=${campaign.bid}")
        val satisfiedBidFloor = impr.bidFloor.getOrElse(0: Double) <= campaign.bid
        //println(s"For banner ${cBanner.id} and impression ${impr.id} impr.w=${impr.w}, impr.wmin=${impr.wmin}, impr.wmax=${impr.wmax}, cBanner.width=${cBanner.width}, widthMatch=${widthMatch}")
        //println(s"For banner ${cBanner.id} and impression ${impr.id} impr.h=${impr.h}, impr.hmin=${impr.hmin}, impr.hmax=${impr.hmax}, cBanner.height=${cBanner.height}, heigthMatch=${heigthMatch}")
        //println(s"For banner ${cBanner.id} and impression ${impr.id} widthMatch=${widthMatch}, heigthMatch=${heigthMatch}, satisfiedBidFloor=${satisfiedBidFloor}")
        //List(impr.w, impr.wmin, impr.wmax).contains(cBanner.width) && List(impr.h, impr.hmin, impr.hmax).contains(cBanner.height)
        widthMatch && heigthMatch && satisfiedBidFloor
      })
      //println(s"Matching impression for banner $cBanner is: $matchingImp")
      matchingImp.nonEmpty
    })
    //println(s"Matching banner is ${matchingBanner.getOrElse(0)}")
    matchingBanner
  }

  private def getBidRequestCountries(bidRequest: BidRequest): Option[List[String]] = {
    var countries = List.empty[String]
    bidRequest.user match{
      case Some(User(id, geo)) => geo match{
        case Some(Geo(country, city, lat, lon)) => country match{
          case Some(con) => countries = con::countries
        }
      }
    }

    bidRequest.device match{
      case Some(Device(id, geo)) => geo match{
        case Some(Geo(country, city, lat, lon)) => country match{
          case Some(con) => countries = con::countries
        }
      }
    }

    Some(countries)
  }

  private def getBidRequestCities(bidRequest: BidRequest): Option[List[String]] = {
    var cities = List.empty[String]
    bidRequest.user match{
      case Some(User(id, geo)) => geo match{
        case Some(Geo(country, city, lat, lon)) => city match{
          case Some(cit) => cities = cit::cities
        }
      }
    }

    bidRequest.device match{
      case Some(Device(id, geo)) => geo match{
        case Some(Geo(country, city, lat, lon)) => city match{
          case Some(cit) => cities = cit::cities
        }
      }
    }

    Some(cities)
  }

  class Auction extends Actor with ActorLogging {
    val campaigns = initCampaigns()
    //val bidRequest = initBidRequest()
    var bids = List.empty[Bid]
    def receive = {
      case bidRequest @ BidRequest(id, imp, site, user, device) =>
        val matchingCampaigns: Option[List[Campaign]] = matchCampaign(bidRequest, campaigns)
        log.info(s"Matching campaigns: ${matchingCampaigns}")
        val bidResponse = getBidResponse(matchingCampaigns, bidRequest)
        sender() ! bidResponse

      case _  => log.info("Invalid message")
    }
  }

  private def getBidResponse(matchingCampaigns: Option[List[Campaign]], bidRequest: BidRequest): Option[BidResponse] = {
    //println(s"Matching campaigns: $matchingCampaigns")
    matchingCampaigns match {
      case None => return None
      case Some(campaigns) =>
        val maxValue = campaigns.map(campaign => campaign.bid).max
        val winCampaign: Campaign = campaigns.find(_.bid == maxValue).get

        //Construct Bid Response
        bidResponseCounter += 1
        val banner = findMatchingBanner(winCampaign, bidRequest.imp)
        //Here adid is set to None, as I dont know where to get that value
        val bidResponse: BidResponse = BidResponse(bidResponseCounter.toString, bidRequest.id, winCampaign.bid, None, banner)
        Some(bidResponse)
    }
  }


  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)
  implicit val impressionFormat = jsonFormat8(Impression)
  implicit val geoFormat = jsonFormat4(Geo)
  implicit val userFormat = jsonFormat2(User)
  implicit val siteFormat = jsonFormat2(Site)
  implicit val deviceFormat = jsonFormat2(Device)
  implicit val bidRequestFormat = jsonFormat5(BidRequest)
  implicit val bannerFormat = jsonFormat4(Banner)
  implicit val bidResponseFormat = jsonFormat5(BidResponse)

  private def initBidRequest(): BidRequest ={
    val geo: Geo = Geo(Some("India"), Some("Mumbai"), Some(24.30), Some(77.79))
    val device: Device = Device("device1", Some(geo))
    val user: User = User("user1", Some(geo))
    val site: Site = Site(1, "eskimi.com")
    val impression1: Impression = Impression("impression1", Some(100), Some(300), Some(200), Some(100), Some(300), Some(200), Some(0.1))
    val impression2: Impression = Impression("impression2", Some(100), Some(300), Some(200), Some(100), Some(300), Some(200), Some(0.2))
    val impression3: Impression = Impression("impression3", Some(100), Some(300), Some(200), Some(100), Some(300), Some(200), Some(0.3))
    val bidRequest: BidRequest = BidRequest("bidrequest1", Some(List(impression1, impression2, impression3)), site, Some(user), Some(device))
    bidRequest
  }

  private def initCampaigns(): List[Campaign] ={
    val banner1: Banner = Banner(1, "Source1", 300, 100)
    val banner2: Banner = Banner(2, "Source2", 100, 300)
    val banner3: Banner = Banner(3, "Source3", 200, 200)

    val targeting1: Targeting = Targeting(List("Kaunas", "Vilnius", "Klaipeda"), List(1, 2, 3))
    val targeting2: Targeting = Targeting(List("Dhaka", "Chittagong", "Sylhet"), List(4, 5, 6))
    val targeting3: Targeting = Targeting(List("Mumbai", "Bangalore", "Hyderabad"), List(7, 8, 9))

    val campaign1: Campaign = Campaign(1, 1, "Lithuania", targeting1, List(banner1, banner2, banner3), 0.1)
    val campaign2: Campaign = Campaign(2, 2, "Bangladesh", targeting2, List(banner1, banner2, banner3), 0.2)
    val campaign3: Campaign = Campaign(3, 3, "India", targeting3, List(banner1, banner2, banner3), 0.3)

    List(campaign1, campaign2, campaign3)
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val auction = system.actorOf(Props[Auction], "auction")

    val route =
      path("auction") {
        post {
          entity(as [BidRequest]) { bidRequest =>
            implicit val timeout: Timeout = 5.seconds
            val bidResponse: Future[Option[BidResponse]] = (auction ? bidRequest).mapTo[Option[BidResponse]]

            onSuccess(bidResponse) {
              case Some(response) => complete(response)
              case None       => complete(StatusCodes.NoContent)
            }
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
}
