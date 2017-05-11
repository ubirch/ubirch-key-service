package com.ubirch.key.model.db

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2017-05-11
  */
case class PublicKey(pubKeyInfo: PublicKeyInfo,
                     signature: String,
                     previousPubKeySignature: Option[String]
                    )

case class PublicKeyInfo(hwDeviceId: String,
                         pubKey: String,
                         pubKeyId: String,
                         algorithm: String,
                         previousPubKeyId: Option[String],
                         created: DateTime,
                         validNotBefore: DateTime,
                         validNotAfter: Option[DateTime] = None
                        )
