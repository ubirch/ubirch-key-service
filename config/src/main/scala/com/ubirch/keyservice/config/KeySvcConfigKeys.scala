package com.ubirch.keyservice.config

/**
  * author: cvandrei
  * since: 2017-01-19
  */
object KeySvcConfigKeys {

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

  final val SEARCH_TRUSTED_MAX_DEPTH = s"$CONFIG_PREFIX.searchTrusted.maxDepth"

  /*
   * Akka
   *********************************************************************************************/

  private val akkaPrefix = s"$CONFIG_PREFIX.akka"

  final val ACTOR_TIMEOUT = s"$akkaPrefix.actorTimeout"
  final val AKKA_NUMBER_OF_WORKERS = s"$akkaPrefix.numberOfWorkers"

  /*
   * Neo4j
   ************************************************************************************************/

  final val neo4jConfigPrefix: String = "ubirchKeyService.neo4j"

}
