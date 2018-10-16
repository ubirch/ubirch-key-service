// see http://www.scala-sbt.org/0.13/docs/Parallel-Execution.html for details
concurrentRestrictions in Global := Seq(
  Tags.limit(Tags.Test, 1)
)

val commonSettings = Seq(

  scalaVersion := "2.11.12",
  scalacOptions ++= Seq("-feature"),
  organization := "com.ubirch.key",

  homepage := Some(url("http://ubirch.com")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/ubirch/ubirch-key-service"),
    "scm:git:git@github.com:ubirch/ubirch-key-service.git"
  )),
  version := "0.11.1",
  test in assembly := {},
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")//,
    //Resolver.sonatypeRepo("snapshots")
  )

)

/*
 * MODULES
 ********************************************************/

lazy val keyService = (project in file("."))
  .settings(
    commonSettings,
    skip in publish := true
  )
  .aggregate(
    clientRest,
    clientRestCacheRedis,
    cmdtools,
    config,
    core,
    modelDb,
    modelRest,
    server,
    testTools,
    util,
    utilsNeo4j
  )

lazy val clientRest = (project in file("client-rest"))
  .settings(commonSettings)
  .dependsOn(
    config,
    modelRest,
    util,
    core % "test",
    testTools % "test"
  )
  .settings(
    name := "client-rest",
    description := "REST client of the key-service",
    libraryDependencies ++= depClientRest
  )

lazy val clientRestCacheRedis = (project in file("client-rest-cache-redis"))
  .settings(commonSettings)
  .dependsOn(
    clientRest,
    core % "test",
    testTools % "test"
  )
  .settings(
    name := "client-rest-cache-redis",
    description := "REST client of the key-service (with Redis based cache)",
    libraryDependencies ++= depClientRestCacheRedis
  )

lazy val config = project
  .settings(commonSettings)
  .settings(
    description := "key-service specific config and config tools",
    libraryDependencies ++= depConfig
  )

lazy val cmdtools = project
  .settings(commonSettings)
  .dependsOn(config, modelRest, util, utilsNeo4j, testTools)
  .settings(
    description := "command line tools",
    libraryDependencies ++= depCmdTools
  )

lazy val core = project
  .settings(commonSettings)
  .dependsOn(modelDb, modelRest, utilsNeo4j, util, testTools % "test")
  .settings(
    description := "business logic",
    libraryDependencies ++= depCore
  )

lazy val modelDb = (project in file("model-db"))
  .settings(commonSettings)
  .settings(
    name := "model-db",
    description := "DB models",
    libraryDependencies ++= depModelDb
  )

lazy val modelRest = (project in file("model-rest"))
  .settings(commonSettings)
  .settings(
    name := "model-rest",
    description := "JSON models",
    libraryDependencies ++= depModelRest
  )

lazy val server = project
  .settings(commonSettings)
  .settings(mergeStrategy: _*)
  .dependsOn(config, core, modelDb, modelRest, util, utilsNeo4j, testTools % "test")
  .enablePlugins(DockerPlugin)
  .settings(
    description := "REST interface and Akka HTTP specific code",
    libraryDependencies ++= depServer,
    fork in run := true,
    resolvers ++= Seq(
      resolverSeebergerJson
    ),
    mainClass in(Compile, run) := Some("com.ubirch.keyservice.server.Boot"),
    resourceGenerators in Compile += Def.task {
      generateDockerFile(baseDirectory.value / ".." / "Dockerfile.input", (assemblyOutputPath in assembly).value)
    }.taskValue
  )

lazy val testTools = (project in file("test-tools"))
  .settings(commonSettings)
  .dependsOn(config, modelDb, modelRest, util, utilsNeo4j)
  .settings(
    name := "test-tools",
    description := "tools useful in automated tests",
    libraryDependencies ++= depTestTools
  )

lazy val util = project
  .settings(commonSettings)
  .dependsOn(modelDb, modelRest % "test")
  .settings(
    description := "utils",
    libraryDependencies ++= depUtils
  )

lazy val utilsNeo4j = (project in file("util-neo4j"))
  .settings(commonSettings)
  .dependsOn(config)
  .settings(
    name := "utils-neo4j",
    description := "Neo4j utils",
    libraryDependencies ++= depUtilsNeo4j
  )

/*
 * MODULE DEPENDENCIES
 ********************************************************/

lazy val depClientRest = Seq(
  akkaHttp,
  akkaStream,
  akkaSlf4j,
  ubirchResponse,
  ubirchDeepCheckModel
) ++ scalaLogging

lazy val depClientRestCacheRedis = Seq(
  ubirchUtilRedisUtil,
  ubirchUtilRedisTestUtil % "test"
)

lazy val depConfig = Seq(
  ubirchConfig
)

lazy val depCmdTools = Seq(
) ++ scalaLogging

lazy val depCore = Seq(
  akkaActor,
  akkaSlf4j,
  json4sNative,
  ubirchCrypto,
  ubirchJson,
  ubirchDeepCheckModel,
  ubirchUuid,
  ubirchNeo4jUtils,
  msgpackScala,
  googleGuava,
  scalatest % "test"
) ++ scalaLogging

lazy val depModelDb = Seq(
  ubirchDate,
  ubirchJson,
  ubirchUuid,
  json4sNative,
  scalatest % "test"
)

lazy val depModelRest = Seq(
  ubirchDate,
  ubirchJson,
  ubirchUuid,
  json4sNative,
  scalatest % "test"
)

lazy val depServer = Seq(
  akkaSlf4j,
  akkaHttp,
  akkaStream,
  ubirchRestAkkaHttp,
  ubirchResponse,
  ubirchRestAkkaHttpTest % "test"
)

lazy val depTestTools = Seq(
  json4sNative,
  ubirchCrypto,
  ubirchJson,
  ubirchUuid,
  ubirchNeo4jUtils,
  scalatest
) ++ scalaLogging

lazy val depUtils = Seq(
  ubirchUuid,
  ubirchCrypto,
  scalatest % "test"
) ++ scalaLogging

lazy val depUtilsNeo4j = Seq(
  ubirchNeo4jUtils
) ++ scalaLogging ++ joda

/*
 * DEPENDENCIES
 ********************************************************/

// VERSIONS
val akkaV = "2.5.11"
val akkaHttpV = "10.1.3"
val json4sV = "3.6.0"

val scalaTestV = "3.0.5"

val logbackV = "1.2.3"
val logbackESV = "1.5"
val slf4jV = "1.7.25"
val log4jV = "2.9.1"
val scalaLogV = "3.7.2"
val scalaLogSLF4JV = "2.1.2"


// GROUP NAMES
val ubirchUtilG = "com.ubirch.util"
val json4sG = "org.json4s"
val akkaG = "com.typesafe.akka"

val scalatest = "org.scalatest" %% "scalatest" % scalaTestV

val json4sNative = json4sG %% "json4s-native" % json4sV
val msgpackScala = "org.msgpack" %% "msgpack-scala" % "0.6.11"
val googleGuava = "com.google.guava" % "guava" % "26.0-jre"

val scalaLogging = Seq(
  "org.slf4j" % "slf4j-api" % slf4jV,
  "org.slf4j" % "log4j-over-slf4j" % slf4jV,
  "org.slf4j" % "jul-to-slf4j" % slf4jV,
  "ch.qos.logback" % "logback-core" % logbackV,
  "ch.qos.logback" % "logback-classic" % logbackV,
  "net.logstash.logback" % "logstash-logback-encoder" % "5.0",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % scalaLogSLF4JV,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLogV,
  "com.internetitem" % "logback-elasticsearch-appender" % logbackESV
)

val akkaActor = akkaG %% "akka-actor" % akkaV
val akkaHttp = akkaG %% "akka-http" % akkaHttpV
val akkaSlf4j = akkaG %% "akka-slf4j" % akkaV
val akkaStream = akkaG %% "akka-stream" % akkaV

val jodaTime = "joda-time" % "joda-time" % "2.10"
val jodaConvert = "org.joda" % "joda-convert" % "2.1.1"
val joda = Seq(jodaTime, jodaConvert)

val excludedLoggers = Seq(
  ExclusionRule(organization = "com.typesafe.scala-logging"),
  ExclusionRule(organization = "org.slf4j"),
  ExclusionRule(organization = "ch.qos.logback")
)

val ubirchConfig = ubirchUtilG %% "config" % "0.2.3" excludeAll (excludedLoggers: _*)
val ubirchCrypto = ubirchUtilG %% "crypto" % "0.4.11" excludeAll (excludedLoggers: _*)
val ubirchDate = ubirchUtilG %% "date" % "0.5.3" excludeAll (excludedLoggers: _*)
val ubirchDeepCheckModel = ubirchUtilG %% "deep-check-model" % "0.3.1" excludeAll (excludedLoggers: _*)
val ubirchJson = ubirchUtilG %% "json" % "0.5.1" excludeAll (excludedLoggers: _*)
val ubirchNeo4jUtils = ubirchUtilG %% "neo4j-utils" % "0.2.1" excludeAll (excludedLoggers: _*)
val ubirchUtilRedisTestUtil = ubirchUtilG %% "redis-test-util" % "0.5.2"
val ubirchUtilRedisUtil = ubirchUtilG %% "redis-util" % "0.5.2"
val ubirchResponse = ubirchUtilG %% "response-util" % "0.4.1" excludeAll (excludedLoggers: _*)
val ubirchRestAkkaHttp = ubirchUtilG %% "rest-akka-http" % "0.4.0" excludeAll (excludedLoggers: _*)
val ubirchRestAkkaHttpTest = ubirchUtilG %% "rest-akka-http-test" % "0.4.0" excludeAll (excludedLoggers: _*)
val ubirchUuid = ubirchUtilG %% "uuid" % "0.1.3" excludeAll (excludedLoggers: _*)

/*
 * RESOLVER
 ********************************************************/

val resolverSeebergerJson = Resolver.bintrayRepo("hseeberger", "maven")
//val resolverTypesafeReleases = "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

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