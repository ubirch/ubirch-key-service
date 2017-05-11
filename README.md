# ubirch-key-service


## General Information

This project serves as a keyserver but unlike PGP keyservers it has additional features (for example, uploading pub keys
is only possible if you control the private key, too).


## Release History

### Version 0.1.0 (2017-05-11)

* initial release


## Scala Dependencies

### `cmdtools`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "cmdtools" % "0.1.0"
)
```

### `config`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "config" % "0.1.0"
)
```

### `core`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "core" % "0.1.0"
)
```

### `model-db`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-db" % "0.1.0"
)
```

### `model-rest`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-rest" % "0.1.0"
)
```

### `server`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.bintrayRepo("hseeberger", "maven"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "server" % "0.1.0"
)
```

### `test-tools`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "test-tools" % "0.1.0"
)
```

### `util`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "util" % "0.1.0"
)
```


## REST Methods

### Welcome / Health

    curl localhost:8095/
    curl localhost:8095/api/keyService/v1

If healthy the server response is:

    200 {"version":"1.0","status":"OK","message":"Welcome to the ubirchKeyService"}

If not healthy the server response is:

    400 {"version":"1.0","status":"NOK","message":"$ERROR_MESSAGE"}


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

1) download and install [Neo4j 3.1.x](https://neo4j.com/download/community-edition/) (community edition)

2) start Neo4j

  1) first time setup: set the password to "neo4jneo4j" (as configured in application.base.conf)
  
  2) prepare database
  
    1) clear database
    
      `./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.Neo4jDelete"`
    
    2) create constraints

      `./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.InitData"`

3) start key-service

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
