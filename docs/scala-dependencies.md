## Scala Dependencies

### `client-rest`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "client-rest" % "0.10.0-SNAPSHOT"
)
```

#### Configuration
   
| Config Item                        | Mandatory  | Description       |
|:-----------------------------------|:-----------|:------------------|
| ubirchKeyService.client.rest.host  | yes        | key-service host  |

#### Usage

See `com.ubirch.keyservice.client.rest.KeyServiceClientRestSpec` for an example usage.

The REST client class is `KeyServiceClientRest` and the host it connects to needs to be configured:

    ubirchKeyService.client.rest.host = "http://localhost:8095"

It depends on a `akka-http` client. Please refer to the setup of `KeyServiceClientRestSpec` for further details.

### `client-rest-cache-redis`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "client-rest-cache-redis" % "0.10.0-SNAPSHOT"
)
```

#### Configuration
   
| Config Item                                 | Mandatory  | Description                      |
|:--------------------------------------------|:-----------|:---------------------------------|
| ubirchKeyService.client.rest.host           | yes        | key-service host                 |
| ubirchKeyService.client.redis.cache.maxTTL  | no         | maximum number of seconds public keys can be cached in Redis (default = 600) |
| ubirch.redisUtil.host                       | yes        | Redis cache connection: host     |
| ubirch.redisUtil.port                       | yes        | Redis cache connection: tcp port |
| ubirch.redisUtil.password                   | no         | Redis cache connection: password |

Here's an example:

```
ubirch.redisUtil {
  host = localhost
  port = 6379
}

ubirchKeyService.client {
  rest.host = "http://localhost:8095"
  redis.cache.maxTTL = 600 // seconds
}
```

#### Usage

**This REST client caches the results of some queries in a Redis cache. Hence additional configuration is required
compared to `com.ubirch.key:client-rest`.**

See `com.ubirch.keyservice.client.rest.KeyServiceClientRestCacheRedisSpec` for an example usage.

The REST client class is `KeyServiceClientRest` and the host it connects to needs to be configured:

    ubirchKeyService.client.rest.host = "http://localhost:8095"

The `maxTTL` is optional:

    ubirchKeyService.client.rest.maxTTL = 600 // seconds

The client depends on a `akka-http` client and an `ActorSystem`. Please refer to the setup of
`KeyServiceClientRestCacheRedisSpec` for further details.

### `cmdtools`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "anormcypher" at "http://repo.anormcypher.org/", // needed by dependency org.anormcypher:anormcypher
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/" // needed by dependency org.anormcypher:anormcypher
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "cmdtools" % "0.10.0-SNAPSHOT"
)
```

### `config`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "config" % "0.10.0-SNAPSHOT"
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
  "com.ubirch.key" %% "core" % "0.10.0-SNAPSHOT"
)
```

### `model-db`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-db" % "0.10.0-SNAPSHOT"
)
```

### `model-rest`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-rest" % "0.10.0-SNAPSHOT"
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
  "com.ubirch.key" %% "server" % "0.10.0-SNAPSHOT"
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
  "com.ubirch.key" %% "test-tools" % "0.10.0-SNAPSHOT"
)
```

### `util`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "util" % "0.10.0-SNAPSHOT"
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
  "com.ubirch.key" %% "utilsNeo4j" % "0.10.0-SNAPSHOT"
)
```
