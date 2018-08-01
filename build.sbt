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
  version := "0.7.0-SNAPSHOT",
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
  .settings(
    commonSettings,
    skip in publish := true
  )
  .aggregate(
    clientRest,
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
  .dependsOn(config, modelRest, util, testTools % "test", core % "test")
  .settings(
    name := "client-rest",
    description := "REST client of the key-service",
    libraryDependencies ++= depClientRest
  )

lazy val config = project
  .settings(commonSettings)
  .settings(
    description := "key-service specific config and config tools",
    libraryDependencies += ubirchConfig
  )

lazy val cmdtools = project
  .settings(commonSettings)
  .dependsOn(config, util, utilsNeo4j)
  .settings(
    description := "command line tools",
    libraryDependencies ++= depCmdTools,
    resolvers ++= anormCypherResolvers
  )

lazy val core = project
  .settings(commonSettings)
  .dependsOn(modelDb, modelRest, util, testTools % "test")
  .settings(
    description := "business logic",
    libraryDependencies ++= depCore,
    resolvers ++= anormCypherResolvers
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
    ) ++ anormCypherResolvers,
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
    libraryDependencies ++= depTestTools,
    resolvers ++= anormCypherResolvers
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
  .settings(
    name := "utils-neo4j",
    description := "Neo4j utils",
    libraryDependencies ++= depUtilsNeo4j,
    resolvers ++= anormCypherResolvers
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

lazy val depCmdTools = Seq(
  anormCypher
) ++ scalaLogging

lazy val depCore = Seq(
  akkaActor,
  akkaSlf4j,
  json4sNative,
  ubirchCrypto,
  ubirchJson,
  ubirchDeepCheckModel,
  ubirchUuid,
  anormCypher,
  msgpackScala,
  scalatest % "test"
) ++ scalaLogging

lazy val depModelDb = Seq(
  ubirchDate,
  ubirchJson,
  ubirchUuid,
  json4sNative
)

lazy val depModelRest = Seq(
  ubirchDate,
  ubirchJson,
  ubirchUuid,
  json4sNative
)

lazy val depServer = Seq(
  akkaSlf4j,
  akkaHttp,
  akkaStream,
  anormCypher,
  ubirchRestAkkaHttp,
  ubirchResponse,
  ubirchRestAkkaHttpTest % "test"
)

lazy val depTestTools = Seq(
  json4sNative,
  ubirchCrypto,
  ubirchJson,
  ubirchUuid,
  anormCypher,
  scalatest
) ++ scalaLogging

lazy val depUtils = Seq(
  ubirchUuid,
  ubirchCrypto,
  scalatest % "test"
) ++ scalaLogging

lazy val depUtilsNeo4j = Seq(
  anormCypher
) ++ scalaLogging

/*
 * DEPENDENCIES
 ********************************************************/

// VERSIONS
val akkaV = "2.5.11"
val akkaHttpV = "10.1.3"
val json4sV = "3.6.0"
val anormCypherV = "0.10.0"

val scalaTestV = "3.0.1"

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

val anormCypher = "org.anormcypher" %% "anormcypher" % anormCypherV

val json4sNative = json4sG %% "json4s-native" % json4sV
val msgpackScala = "org.msgpack" %% "msgpack-scala" % "0.6.11"

val scalaLogging = Seq(
  "org.slf4j" % "slf4j-api" % slf4jV,
  "org.slf4j" % "log4j-over-slf4j" % slf4jV,
  "org.slf4j" % "jul-to-slf4j" % slf4jV,
  "ch.qos.logback" % "logback-core" % logbackV,
  "ch.qos.logback" % "logback-classic" % logbackV,
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % scalaLogSLF4JV,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLogV,
  "com.internetitem" % "logback-elasticsearch-appender" % logbackESV
)

val akkaActor = akkaG %% "akka-actor" % akkaV
val akkaHttp = akkaG %% "akka-http" % akkaHttpV
val akkaSlf4j = akkaG %% "akka-slf4j" % akkaV
val akkaStream = akkaG %% "akka-stream" % akkaV

val excludedLoggers = Seq(
  ExclusionRule(organization = "com.typesafe.scala-logging"),
  ExclusionRule(organization = "org.slf4j"),
  ExclusionRule(organization = "ch.qos.logback")
)

val ubirchConfig = ubirchUtilG %% "config" % "0.2.1" excludeAll (excludedLoggers: _*)
val ubirchCrypto = ubirchUtilG %% "crypto" % "0.4.9" excludeAll (excludedLoggers: _*)
val ubirchDate = ubirchUtilG %% "date" % "0.5.2" excludeAll (excludedLoggers: _*)
val ubirchDeepCheckModel = ubirchUtilG %% "deep-check-model" % "0.3.0" excludeAll (excludedLoggers: _*)
val ubirchJson = ubirchUtilG %% "json" % "0.5.0" excludeAll (excludedLoggers: _*)
val ubirchResponse = ubirchUtilG %% "response-util" % "0.4.0" excludeAll (excludedLoggers: _*)
val ubirchRestAkkaHttp = ubirchUtilG %% "rest-akka-http" % "0.4.0" excludeAll (excludedLoggers: _*)
val ubirchRestAkkaHttpTest = ubirchUtilG %% "rest-akka-http-test" % "0.4.0" excludeAll (excludedLoggers: _*)
val ubirchUuid = ubirchUtilG %% "uuid" % "0.1.3" excludeAll (excludedLoggers: _*)

/*
 * RESOLVER
 ********************************************************/

val resolverSeebergerJson = Resolver.bintrayRepo("hseeberger", "maven")
val resolverAnormcypher = "anormcypher" at "http://repo.anormcypher.org/"
val resolverTypesafeReleases = "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

val anormCypherResolvers = Seq(resolverAnormcypher, resolverTypesafeReleases)

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