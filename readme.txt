Note: As the purpose of the field "adid" is unknown, it is ignored through out the agent

Sample request 1: This bid would be accepted as it satisfies all the criteria to accept
curl -X POST \
  http://localhost:8080/auction \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 73f59f67-c787-a1cc-4794-61ce27c71f82' \
  -d '{
   "device":{
      "geo":{
         "city":"Mumbai",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"device1"
   },
   "id":"bidrequest1",
   "imp":[
      {
         "bidFloor":0.6,
         "h":200,
         "hmax":300,
         "hmin":100,
         "id":"impression1",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.5,
         "h":200,
         "hmax":300,
         "hmin":100,
         "id":"impression2",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.3,
         "h":200,
         "hmax":300,
         "hmin":500,
         "id":"impression3",
         "w":200
      }
   ],
   "site":{
      "domain":"eskimi.com",
      "id":7
   },
   "user":{
      "geo":{
         "city":"Mumbai",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"user1"
   }
}'


Sample request 2: This bid wont be accepted as it doesn't satisfy with bidFloor parameter
curl -X POST \
  http://localhost:8080/auction \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 61e03a09-1b20-bb51-4792-042ce78218a1' \
  -d '{
   "device":{
      "geo":{
         "city":"Mumbai",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"device1"
   },
   "id":"bidrequest1",
   "imp":[
      {
         "bidFloor":0.6,
         "h":200,
         "hmax":300,
         "hmin":100,
         "id":"impression1",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.5,
         "h":200,
         "hmax":300,
         "hmin":100,
         "id":"impression2",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.4,
         "h":200,
         "hmax":300,
         "hmin":500,
         "id":"impression3",
         "w":200
      }
   ],
   "site":{
      "domain":"eskimi.com",
      "id":7
   },
   "user":{
      "geo":{
         "city":"Mumbai",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"user1"
   }
}'

Sample request 3: This bid wont be accepted because of extreme banner height
curl -X POST \
  http://localhost:8080/auction \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 008df496-040f-9815-c93b-934efffab4ea' \
  -d '{
   "device":{
      "geo":{
         "city":"Mumbai",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"device1"
   },
   "id":"bidrequest1",
   "imp":[
      {
         "bidFloor":0.6,
         "h":200,
         "hmax":300,
         "hmin":100,
         "id":"impression1",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.3,
         "h":400,
         "hmax":600,
         "hmin":500,
         "id":"impression2",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.4,
         "h":200,
         "hmax":300,
         "hmin":500,
         "id":"impression3",
         "w":200
      }
   ],
   "site":{
      "domain":"eskimi.com",
      "id":7
   },
   "user":{
      "geo":{
         "city":"Mumbai",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"user1"
   }
}'

Sample request 4: Although device city doesn't match with campaign, still this bid would be accepted as user city matches with campaign city
curl -X POST \
  http://localhost:8080/auction \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: a2e70d3d-9743-f22b-56f9-e7a0d705d5c8' \
  -d '{
   "device":{
      "geo":{
         "city":"Ahmedabad",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"device1"
   },
   "id":"bidrequest1",
   "imp":[
      {
         "bidFloor":0.6,
         "h":200,
         "hmax":300,
         "hmin":100,
         "id":"impression1",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.3,
         "h":400,
         "hmax":300,
         "hmin":500,
         "id":"impression2",
         "w":200,
         "wmax":300,
         "wmin":100
      },
      {
         "bidFloor":0.4,
         "h":200,
         "hmax":300,
         "hmin":500,
         "id":"impression3",
         "w":200
      }
   ],
   "site":{
      "domain":"eskimi.com",
      "id":7
   },
   "user":{
      "geo":{
         "city":"Mumbai",
         "country":"India",
         "lat":24.3,
         "lon":77.79
      },
      "id":"user1"
   }
}'