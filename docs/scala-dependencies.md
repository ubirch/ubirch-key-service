## Scala Dependencies

### `client-rest`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "client-rest" % "0.2.1-SNAPSHOT"
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
  "com.ubirch.key" %% "cmdtools" % "0.2.1-SNAPSHOT"
)
```

### `config`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "config" % "0.2.1-SNAPSHOT"
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
  "com.ubirch.key" %% "core" % "0.2.1-SNAPSHOT"
)
```

### `model-db`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-db" % "0.2.1-SNAPSHOT"
)
```

### `model-rest`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "model-rest" % "0.2.1-SNAPSHOT"
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
  "com.ubirch.key" %% "server" % "0.2.1-SNAPSHOT"
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
  "com.ubirch.key" %% "test-tools" % "0.2.1-SNAPSHOT"
)
```

### `util`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.key" %% "util" % "0.2.1-SNAPSHOT"
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
  "com.ubirch.key" %% "utilsNeo4j" % "0.2.1-SNAPSHOT"
)
```
