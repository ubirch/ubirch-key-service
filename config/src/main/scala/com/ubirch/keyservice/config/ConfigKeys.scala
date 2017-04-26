package com.ubirch.keyservice.config

/**
  * author: cvandrei
  * since: 2017-01-19
  */
object ConfigKeys {

  final val CONFIG_PREFIX = "ubirchKeyService"

  /*
   * general server configs
   *********************************************************************************************/

  final val INTERFACE = s"$CONFIG_PREFIX.interface"
  final val PORT = s"$CONFIG_PREFIX.port"
  final val TIMEOUT = s"$CONFIG_PREFIX.timeout"

  /*
   * Akka related configs
   *********************************************************************************************/

  private val akkaPrefix = s"$CONFIG_PREFIX.akka"

  final val ACTOR_TIMEOUT = s"$akkaPrefix.actorTimeout"
  final val AKKA_NUMBER_OF_WORKERS = s"$akkaPrefix.numberOfWorkers"

}
