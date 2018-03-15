package com.ubirch.key.model.rest

/**
  * author: cvandrei
  * since: 2017-04-27
  */
case class PublicKey(pubKeyInfo: PublicKeyInfo,
                     signature: String,
                     previousPubKeySignature: Option[String] = None
                    )

case class PublicKeys(publicKeys: Set[PublicKey])
