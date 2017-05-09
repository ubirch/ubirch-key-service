package com.ubirch.key.model.rest

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2017-04-27
  */
case class PublicKeyInfo(hwDeviceId: String,
                         pubKey: String,
                         pubKeyId: String, // hash of "pubKey" (algorithm tends to depend on "algorithm"
                         algorithm: String,
                         previousPubKey: Option[String],
                         created: DateTime,
                         validNotBefore: DateTime,
                         validNotAfter: Option[DateTime] = None
                        )
