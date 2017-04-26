# ubirch-template-service


## General Information

This project can be used as a template for ubirch-services. To actually use it please copy it and replace all
occurrences of the string "template" with the name of your new service.


## Release History

### Version 0.1.0 (tbd)

* initial release


## Scala Dependencies

### `cmdtools`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.template" %% "cmdtools" % "0.1.0-SNAPSHOT"
)
```

### `config`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.template" %% "config" % "0.1.0-SNAPSHOT"
)
```

### `core`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.template" %% "core" % "0.1.0-SNAPSHOT"
)
```

### `model`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.template" %% "model" % "0.1.0-SNAPSHOT"
)
```

### `server`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.bintrayRepo("hseeberger", "maven")
)
libraryDependencies ++= Seq(
  "com.ubirch.template" %% "server" % "0.1.0-SNAPSHOT"
)
```

### `util`

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)
libraryDependencies ++= Seq(
  "com.ubirch.template" %% "util" % "0.1.0-SNAPSHOT"
)
```


## REST Methods

### Welcome / Health

    curl localhost:8092/
    curl localhost:8092/api/templateService/v1

If healthy the server response is:

    200 {"version":"1.0","status":"OK","message":"Welcome to the ubirchTemplateService"}

If not healthy the server response is:

    400 {"version":"1.0","status":"NOK","message":"$ERROR_MESSAGE"}


## Configuration

TODO


## Deployment Notes

TODO


## Automated Tests

run all tests

    ./sbt test

### generate coverage report

    ./sbt coverageReport

more details here: https://github.com/scoverage/sbt-scoverage


## Local Setup

TODO


## Create Docker Image

    ./sbt server/docker
