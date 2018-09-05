package com.ubirch.keyservice.client.rest.cache.redis.config

import com.ubirch.keyservice.client.rest.config.KeyClientRestConfigBase

/**
  * author: cvandrei
  * since: 2018-09-05
  */
object KeyClientRedisConfig extends KeyClientRestConfigBase {

  /**
    * @return maximum time-to-live in seconds for records to cache
    */
  def maxTTL: Int = config.getInt(KeyClientRedisConfigKeys.maxTTL)

}
