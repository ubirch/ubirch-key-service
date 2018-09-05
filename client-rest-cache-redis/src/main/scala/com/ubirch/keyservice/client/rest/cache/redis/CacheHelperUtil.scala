package com.ubirch.keyservice.client.rest.cache.redis

/**
  * author: cvandrei
  * since: 2018-09-05
  */
object CacheHelperUtil {

  def cacheKeyPublicKey(publicKey: String): String = s"keyService.cache.publicKey.$publicKey"

  def cacheKeyHardwareId(hardwareId: String): String = s"keyService.cache.hardwareId.$hardwareId"

}
