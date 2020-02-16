name := "Realtime Bidding Agent"

version := "1.0"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "io.spray" %%  "spray-json" % "1.3.5",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)
