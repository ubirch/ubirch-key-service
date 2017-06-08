package com.ubirch.keyservice.config

/**
  * author: cvandrei
  * since: 2017-01-19
  */
object ConfigKeys {

  final val CONFIG_PREFIX = "ubirchKeyService"

  /*
   * this service
   *********************************************************************************************/

  final val INTERFACE = s"$CONFIG_PREFIX.interface"
  final val PORT = s"$CONFIG_PREFIX.port"
  final val TIMEOUT = s"$CONFIG_PREFIX.timeout"

  final val GO_PIPELINE_NAME = s"$CONFIG_PREFIX.gopipelinename"
  final val GO_PIPELINE_LABEL = s"$CONFIG_PREFIX.gopipelinelabel"
  final val GO_PIPELINE_REVISION = s"$CONFIG_PREFIX.gopipelinerev"

  /*
   * Akka
   *********************************************************************************************/

  private val akkaPrefix = s"$CONFIG_PREFIX.akka"

  final val ACTOR_TIMEOUT = s"$akkaPrefix.actorTimeout"
  final val AKKA_NUMBER_OF_WORKERS = s"$akkaPrefix.numberOfWorkers"

  /*
   * Neo4j
   *********************************************************************************************/

  private val neo4jPrefix = s"$CONFIG_PREFIX.neo4j"

  final val NEO4J_HOST = s"$neo4jPrefix.host"
  final val NEO4J_PORT = s"$neo4jPrefix.port"
  final val NEO4J_USER_NAME = s"$neo4jPrefix.userName"
  final val NEO4J_PASSWORD = s"$neo4jPrefix.password"
  final val NEO4J_HTTPS = s"$neo4jPrefix.https"

}
