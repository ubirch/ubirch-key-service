package com.ubirch.key.model.rest

/**
  * author: cvandrei
  * since: 2017-04-27
  */
case class PublicKey(pubkeyInfo: PublicKeyInfo,
                     signature: String,
                     previousPubKeySignature: Option[String]
                    )

case class PublicKeys(publicKeys: Set[PublicKey])
