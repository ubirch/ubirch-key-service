# ubirch-key-service


## General Information

This project serves as a keyserver but unlike PGP keyservers it has additional features (for example, uploading pub keys
is only possible if you control the private key, too).


## Release History

### Version 0.1.13 (tbd)

* update to `com.ubirch.util:json:0.4.3`
* update to `com.ubirch.util:deep-check-model:0.1.3`
* update to `com.ubirch.util:response-util:0.2.4`

### Version 0.1.12 (2017-07-24)

* revert to Akka 2.4.19
* revert to Play 2.4.11
* revert to anormcypher 0.9.1
* refactor `KeyServiceClientRest` to use Akka Http for the connection

### Version 0.1.11 (2017-07-18)

* add `resetDatabase.sh` script
* update to Akka 2.4.19
* update to Play 2.5.3
* update to anormcypher 0.10.0

### Version 0.1.10 (2017-07-17)

* update Akka HTTP to 10.0.9
* update _com.ubirch.util:rest-akka-http(-test)_ to 0.3.8
* update _com.ubirch.util:response-util_ to 0.2.3

### Version 0.1.9 (2017-07-17)

* extract `Neo4jSchema` and `Neo4jUtils` to new artifact `com.ubirch.key:utils-neo4j`

### Version 0.1.8 (2017-07-12)

* update logging dependencies
* update logback configs

### Version 0.1.7 (2017-06-29)

* refactored actors by adding a `props()` method
* updated to _com.ubirch.util:json:0.4.2_ and all ubirch util libs depending on it, too

### Version 0.1.6 (2017-06-21)

* add module `client-rest`

### Version 0.1.5 (2017-06-19)

* queries in `PublicKeyManager` are now asynchronous
* update json4s to version 3.5.2
* update to _com.ubirch.util:deep-check-model:0.1.1_
* update to _com.ubirch.util:json:0.4.1_
* update to _com.ubirch.util:response-util:0.2.1_

### Version 0.1.4 (2017-06-12)

* endpoint `/api/userService/v1/deepCheck` responds with http status 503 if deep check finds problems

### Version 0.1.3 (2017-06-09)

* migrate to _com.ubirch.util:deep-check-model:0.1.0_

### Version 0.1.2 (2017-06-08)

* introduce endpoint `/api/authService/v1/check`
* update to sbt 0.13.15
* added `PublicKeyUtil`
* update _com.ubirch.util:json_ to version 0.4.0
* update _com.ubirch.util:response-util_ to version 0.1.6
* introduce endpoint `/api/userService/v1/deepCheck`

### Version 0.1.1 (2017-05-18)

* update Akka Http to 10.0.6
* update Akka to 2.4.18

### Version 0.1.0 (2017-05-11)

* initial release


## Scala Dependencies

### `client-rest`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "client-rest" % "0.1.13-SNAPSHOT"
)
```

#### Configuration
   
| Config Item                        | Mandatory  | Description       |
|:-----------------------------------|:-----------|:------------------|
| ubirchKeyService.client.rest.host  | yes        | key-service host  |

#### Usage

See `com.ubirch.keyservice.client.rest.KeyServiceClientRestSpec` for an example usage.

#### Usage

The REST client class is `KeyServiceClientRest` and the host it connects to needs to be configured:

    ubirchKeyService.client.rest.host = "http://localhost:8095"

It depends on a `akka-http` client. Please refer to the setup of `KeyServiceClientRestSpec` for further details.

### `cmdtools`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "cmdtools" % "0.1.13-SNAPSHOT"
)
```

### `config`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "config" % "0.1.13-SNAPSHOT"
)
```

### `core`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "core" % "0.1.13-SNAPSHOT"
)
```

### `model-db`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-db" % "0.1.13-SNAPSHOT"
)
```

### `model-rest`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-rest" % "0.1.13-SNAPSHOT"
)
```

### `server`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.bintrayRepo("hseeberger", "maven"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "server" % "0.1.13-SNAPSHOT"
)
```

### `test-tools`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "test-tools" % "0.1.13-SNAPSHOT"
)
```

### `util`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "util" % "0.1.13-SNAPSHOT"
)
```

### `utils-neo4j`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "utilsNeo4j" % "0.1.13-SNAPSHOT"
)
```


## REST Methods

### Welcome / Health / Check

    curl localhost:8095/
    curl localhost:8095/api/keyService/v1
    curl localhost:8095/api/keyService/v1/check

If healthy the server response is:

    200 {"version":"1.0","status":"OK","message":"Welcome to the ubirchKeyService ( $GO_PIPELINE_NAME / $GO_PIPELINE_LABEL / $GO_PIPELINE_REVISION )"}

If not healthy the server response is:

    400 {"version":"1.0","status":"NOK","message":"$ERROR_MESSAGE"}

### Deep Check / Server Health

    curl localhost:8092/api/keyService/v1/deepCheck

If healthy the response is:

  F  200 {"status":true,"messages":[]}

If not healthy the status is `false` and the `messages` array not empty:

    503 {"status":false,"messages":["unable to connect to the database"]}


### Public Key

#### Create

    curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '{
      "pubKeyInfo": {
        "hwDeviceId": "some-id-asdf", // String (not always a UUID)
        "pubKey": "string", // base64
        "pubKeyId": "string", // (optional) typically the hash of "pubKey" (algorithm tends to depend on "algorithm") but can also be the _pubKey_, too (useful for ECC keys whose hash would otherwise be longer than the actual key)
        "algorithm": "RSA4096", // check X.509 re: constants
        "previousPubKeyId": "string", // (optional) id of previous pub key
        "created": "2017-04-26T17:18:00.000Z+02:00",
        "validNotBefore": "2017-04-26T17:18:00.000Z+02:00",
        "validNotAfter": "2019-04-26T17:18:00.000Z+02:00" // (optional)
      },
      "signature": "string", // base64 (self signed)
      "previousPubKeySignature": "..." // (optional)
    }'

If successful the response is:

    200
    {
      "pubKeyInfo": {
        "hwDeviceId": "some-id-asdf", // String (not always a UUID)
        "pubKey": "string", // base64
        "pubKeyId": "string", // (optional) typically the hash of "pubKey" (algorithm tends to depend on "algorithm") but can also be the _pubKey_, too (useful for ECC keys whose hash would otherwise be longer than the actual key)
        "algorithm": "RSA4096", // check X.509 re: constants
        "previousPubKeyId": "...", // (optional) String - pub key id
        "created": "2017-04-26T17:18:00.000Z+02:00",
        "validNotBefore": "2017-04-26T17:18:00.000Z+02:00",
        "validNotAfter": "2019-04-26T17:18:00.000Z+02:00" // (optional)
      },
      "signature": "string", // base64 (self signed)
      "previousPubKeySignature": "..." // (optional)
    }

In case of an error the response is:

    400
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "CreateError",
        "errorMessage": "failed to create public key"
      }
    }

If the server has problems the response is:

    500
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "ServerError",
        "errorMessage": "failed to create public key"
      }
    }

#### Query Public Keys by HardwareId (currently active only)

    curl localhost:8095/api/keyService/v1/pubkey/current/hardwareId/$HARDWARE_ID

If no currently valid public keys were found the response is:

    200
    []

If currently valid public keys were found the response is:

    200
    [
      {
        "pubKeyInfo": {
          "hwDeviceId": "some-id-asdf", // String (not always a UUID)
          "pubKey": "string", // base64
          "pubKeyId": "string", // (optional) typically the hash of "pubKey" (algorithm tends to depend on "algorithm") but can also be the _pubKey_, too (useful for ECC keys whose hash would otherwise be longer than the actual key)
          "algorithm": "RSA4096", // check X.509 re: constants
          "previousPubKeyId": "...", // (optional) pub key id
          "created": "2017-04-26T17:18:00.000Z+02:00",
          "validNotBefore": "2017-04-26T17:18:00.000Z+02:00",
          "validNotAfter": "2019-04-26T17:18:00.000Z+02:00" // (optional)
        },
        "signature": "string", // base64 (self signed)
        "previousPubKeySignature": "..." // (optional)
      }
    ]

In case of an error the response is:

    400
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "QueryError",
        "errorMessage": "failed to query public keys"
      }
    }

If the server has problems the response is:

    500
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "ServerError",
        "errorMessage": "failed to query public keys"
      }
    }

## Configuration

| Config Item                      | Mandatory  | Description                           |
|:---------------------------------|:-----------|:--------------------------------------|
| ubirchKeyService.neo4j.host      | no         | Neo4j host (defaults to "localhost")  |
| ubirchKeyService.neo4j.port      | no         | Neo4j port (defaults to 7474)         |
| ubirchKeyService.neo4j.userName  | no         | Neo4j user name (defaults to "")      |
| ubirchKeyService.neo4j.password  | no         | Neo4j password (defaults to "")       |
| ubirchKeyService.neo4j.https     | no         | Neo4j password (defaults to "")       |


## Deployment Notes

This service has the following dependencies:

* Neo4j 3.1.x (constraints and indices are created during server startup)


## Automated Tests

run all tests

    ./sbt test

### generate coverage report

    ./sbt coverageReport

more details here: https://github.com/scoverage/sbt-scoverage


## Local Setup

1. download and install [Neo4j 3.1.x](https://neo4j.com/download/community-edition/) (community edition)

1. start Neo4j

    1. first time setup: set the password to "neo4jneo4j" (as configured in application.base.conf)
  
        1. https://neo4j.com/docs/operations-manual/current/configuration/set-initial-password/

    1. prepare database
    
        *Running `dev-scripts/resetDatabase.sh` does everything in this step.*
  
        1. clear database
    
            `./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.Neo4jDelete"`
    
        1. create constraints

            `./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.InitData"`

1. start key-service

    ./sbt server/run

### Useful Cypher Queries

#### Delete

    MATCH (n)-[r]-(m) DELETE n, r, m // delete all nodes with a relationship to a nother node
    MATCH (n) DELETE n // all nodes
    MATCH (n: PublicKey) DELETE n // all public keys

#### List

    MATCH (pubKey: PublicKey) RETURN pubKey // all public keys


## Create Docker Image

    ./goBuild.sh assembly
