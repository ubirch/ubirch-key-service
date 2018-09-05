package com.ubirch.keyservice.client.rest.cache.redis.config

import com.ubirch.keyservice.client.rest.config.KeyClientRestConfigKeysBase

/**
  * author: cvandrei
  * since: 2018-09-05
  */
object KeyClientRedisConfigKeys extends KeyClientRestConfigKeysBase {

  val maxTTL = s"$base.redis.cache.maxTTL" // seconds

}
