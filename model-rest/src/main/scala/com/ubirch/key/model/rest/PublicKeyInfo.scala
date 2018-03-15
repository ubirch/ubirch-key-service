package com.ubirch.key.model.rest

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2017-04-27
  */

/**
  * @param pubKeyId typically the hash of "pubKey" (algorithm tends to depend on "algorithm") but can also be the _pubKey_, too (useful for ECC keys whose hash would otherwise be longer than the actual key)
  */
case class PublicKeyInfo(algorithm: String,
                         created: DateTime,
                         hwDeviceId: String,
                         previousPubKeyId: Option[String],
                         pubKey: String,
                         pubKeyId: String,
                         validNotAfter: Option[DateTime] = None,
                         validNotBefore: DateTime
                        )
