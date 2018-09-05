package com.ubirch.keyservice.client.rest.config

/**
  * author: cvandrei
  * since: 2017-06-20
  */
object KeyClientRestConfigKeys extends KeyClientRestConfigKeysBase {}

trait KeyClientRestConfigKeysBase {

  protected val base = "ubirchKeyService.client"

  val HOST = s"$base.rest.host"

}
