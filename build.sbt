packagedArtifacts in file(".") := Map.empty // disable publishing of root/default project

// see http://www.scala-sbt.org/0.13/docs/Parallel-Execution.html for details
concurrentRestrictions in Global := Seq(
  Tags.limit(Tags.Test, 1)
)

val projectVersion = "0.1.0-SNAPSHOT"
lazy val commonSettings = Seq(

  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-feature"),
  organization := "com.ubirch.key",

  homepage := Some(url("http://ubirch.com")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/ubirch/ubirch-key-service"),
    "scm:git:git@github.com:ubirch/ubirch-key-service.git"
  )),
  version := "0.1.1-SNAPSHOT",
  test in assembly := {},
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )

)

/*
 * MODULES
 ********************************************************/

lazy val keyService = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(
    cmdtools,
    config,
    core,
    modelDb,
    modelRest,
    server,
    testTools,
    util
  )

lazy val config = project
  .settings(commonSettings: _*)
  .settings(
    description := "key-service specific config and config tools",
    libraryDependencies += ubirchConfig
  )

lazy val cmdtools = project
  .settings(commonSettings: _*)
  .dependsOn(config, util)
  .settings(
    description := "command line tools",
    libraryDependencies ++= depCmdTools,
    resolvers ++= anormCypherResolvers
  )

lazy val core = project
  .settings(commonSettings: _*)
  .dependsOn(modelDb, util, testTools % "test")
  .settings(
    description := "business logic",
    libraryDependencies ++= depCore,
    resolvers ++= anormCypherResolvers
  )

lazy val modelDb = (project in file("model-db"))
  .settings(commonSettings: _*)
  .settings(
    name := "model-db",
    description := "DB models",
    libraryDependencies ++= depModelDb
  )

lazy val modelRest = (project in file("model-rest"))
  .settings(commonSettings: _*)
  .settings(
    name := "model-rest",
    description := "JSON models",
    libraryDependencies ++= depModelRest
  )

lazy val server = project
  .settings(commonSettings: _*)
  .settings(mergeStrategy: _*)
  .dependsOn(config, core, modelDb, modelRest, util, testTools % "test")
  .enablePlugins(DockerPlugin)
  .settings(
    description := "REST interface and Akka HTTP specific code",
    libraryDependencies ++= depServer,
    fork in run := true,
    resolvers ++= Seq(
      resolverSeebergerJson
    ) ++ anormCypherResolvers,
    mainClass in(Compile, run) := Some("com.ubirch.keyservice.server.Boot"),
    resourceGenerators in Compile += Def.task {
      generateDockerFile(baseDirectory.value / ".." / "Dockerfile.input", (assemblyOutputPath in assembly).value)
    }.taskValue
  )

lazy val testTools = (project in file("test-tools"))
  .settings(commonSettings: _*)
  .dependsOn(config, modelDb, modelRest, util)
  .settings(
    name := "test-tools",
    description := "tools useful in automated tests",
    libraryDependencies ++= depTestTools,
    resolvers ++= anormCypherResolvers
  )

lazy val util = project
  .settings(commonSettings: _*)
  .dependsOn()
  .settings(
    description := "utils",
    libraryDependencies ++= depUtils,
    resolvers ++= anormCypherResolvers
  )

/*
 * MODULE DEPENDENCIES
 ********************************************************/

lazy val depCmdTools = Seq(
  anormCypher
) ++ scalaLogging

lazy val depCore = Seq(
  akkaActor,
  json4sNative,
  ubirchCrypto,
  ubirchJson,
  ubirchResponse,
  anormCypher,
  ubirchFutures % "test",
  scalatest % "test"
) ++ scalaLogging

lazy val depModelDb = Seq(
  ubirchDate,
  ubirchJsonAutoConvert,
  json4sNative
)

lazy val depModelRest = Seq(
  ubirchDate,
  ubirchJsonAutoConvert,
  json4sNative
)

lazy val depServer = Seq(

  akkaSlf4j,
  akkaHttp,
  ubirchRestAkkaHttp,
  ubirchRestAkkaHttpTest % "test",

  anormCypher,

  ubirchJsonAutoConvert,
  ubirchResponse

)

lazy val depTestTools = Seq(
  json4sNative,
  ubirchCrypto,
  ubirchJsonAutoConvert,
  ubirchUuid,
  anormCypher,
  scalatest
) ++ scalaLogging

lazy val depUtils = Seq(
  anormCypher
) ++ scalaLogging

/*
 * DEPENDENCIES
 ********************************************************/

// VERSIONS
val akkaV = "2.4.17"
val akkaHttpV = "10.0.6"
val json4sV = "3.5.1"

val scalaTestV = "3.0.1"

// GROUP NAMES
val ubirchUtilG = "com.ubirch.util"
val json4sG = "org.json4s"
val akkaG = "com.typesafe.akka"

lazy val scalatest = "org.scalatest" %% "scalatest" % scalaTestV

lazy val json4sNative = json4sG %% "json4s-native" % json4sV

lazy val scalaLogging = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" exclude("org.slf4j", "slf4j-api"),
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0" exclude("org.slf4j", "slf4j-api"),
  "ch.qos.logback" % "logback-core" % "1.1.7",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.internetitem" % "logback-elasticsearch-appender" % "1.4"
)

lazy val akkaActor = akkaG %% "akka-actor" % akkaV
lazy val akkaHttp = akkaG %% "akka-http" % akkaHttpV
lazy val akkaSlf4j = akkaG %% "akka-slf4j" % akkaV

lazy val excludedLoggers = Seq(
  ExclusionRule(organization = "com.typesafe.scala-logging"),
  ExclusionRule(organization = "org.slf4j"),
  ExclusionRule(organization = "ch.qos.logback")
)

lazy val ubirchConfig = ubirchUtilG %% "config" % "0.1" excludeAll (excludedLoggers: _*)
lazy val ubirchCrypto = ubirchUtilG %% "crypto" % "0.3.3" excludeAll (excludedLoggers: _*)
lazy val ubirchDate = ubirchUtilG %% "date" % "0.1" excludeAll (excludedLoggers: _*)
lazy val ubirchFutures = ubirchUtilG %% "futures" % "0.1.1" excludeAll (excludedLoggers: _*)
lazy val ubirchJson = ubirchUtilG %% "json" % "0.3.4" excludeAll (excludedLoggers: _*)
lazy val ubirchJsonAutoConvert = ubirchUtilG %% "json-auto-convert" % "0.3.4" excludeAll (excludedLoggers: _*)
lazy val ubirchRestAkkaHttp = ubirchUtilG %% "rest-akka-http" % "0.3.6-SNAPSHOT" excludeAll (excludedLoggers: _*)
lazy val ubirchRestAkkaHttpTest = ubirchUtilG %% "rest-akka-http-test" % "0.3.6-SNAPSHOT" excludeAll (excludedLoggers: _*)
lazy val ubirchResponse = ubirchUtilG %% "response-util" % "0.1.4-SNAPSHOT" excludeAll (excludedLoggers: _*)
lazy val ubirchUuid = ubirchUtilG %% "uuid" % "0.1.1" excludeAll (excludedLoggers: _*)

lazy val anormCypher = "org.anormcypher" %% "anormcypher" % "0.9.1"

/*
 * RESOLVER
 ********************************************************/

lazy val resolverSeebergerJson = Resolver.bintrayRepo("hseeberger", "maven")
lazy val resolverAnormcypher = "anormcypher" at "http://repo.anormcypher.org/"
lazy val resolverTypesafeReleases = "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
lazy val anormCypherResolvers = Seq(resolverAnormcypher, resolverTypesafeReleases)

/*
 * MISC
 ********************************************************/

lazy val mergeStrategy = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("org", "joda", "time", xs@_*) => MergeStrategy.first
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
    case m if m.toLowerCase.endsWith("application.conf") => MergeStrategy.concat
    case m if m.toLowerCase.endsWith("application.dev.conf") => MergeStrategy.first
    case m if m.toLowerCase.endsWith("application.base.conf") => MergeStrategy.first
    case m if m.toLowerCase.endsWith("logback.xml") => MergeStrategy.first
    case m if m.toLowerCase.endsWith("logback-test.xml") => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  }
)

def generateDockerFile(file: File, jarFile: sbt.File): Seq[File] = {
  val contents =
    s"""SOURCE=server/target/scala-2.11/${jarFile.getName}
       |TARGET=${jarFile.getName}
       |""".stripMargin
  IO.write(file, contents)
  Seq(file)
}