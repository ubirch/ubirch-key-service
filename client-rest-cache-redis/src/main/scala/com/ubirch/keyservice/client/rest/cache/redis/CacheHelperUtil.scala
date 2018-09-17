package com.ubirch.keyservice.client.rest.cache.redis

/**
  * author: cvandrei
  * since: 2018-09-05
  */
object CacheHelperUtil {

  private val keyRoot = "keyService.cache"

  def cacheKeyPublicKey(publicKey: String): String = s"$keyRoot.publicKey.$publicKey"

  def cacheKeyHardwareId(hardwareId: String): String = s"$keyRoot.hardwareId.$hardwareId"

  def cacheKeyFindTrusted(publicKey: String,
                          depth: Int,
                          minTrust: Int
                         ): String = {

    s"$keyRoot.findTrusted.$publicKey.$depth.$minTrust"

  }

}
