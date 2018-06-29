package com.ubirch.key.model.rest

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2017-04-27
  */

/**
  * @param pubKeyInfo public key details
  * @param signature signature of field _pubKeyInfo_ created with current key pair
  * @param previousPubKeySignature (optional) signature of field _pubKeyInfo_ created with previous key pair
  * @param raw the raw data of the public key structure (in case it is binary)
  */
case class PublicKey(pubKeyInfo: PublicKeyInfo,
                     signature: String,
                     previousPubKeySignature: Option[String] = None,
                     raw: Option[String] = None
                    )

/**
  * @param algorithm        algorithm used to generate public key (defaults to "ed25519-sha-512")
  * @param created          UTC timestamp
  * @param hwDeviceId       hardware device id this public key belongs to
  * @param previousPubKeyId (optional) public key id of previous key
  * @param pubKey           base 64 encoded public key
  * @param pubKeyId         (optional) unique public key id
  * @param validNotAfter    (optional) UTC timestamp until when PublicKey is valid
  * @param validNotBefore   UTC timestamp since when PublicKey is valid
  */
case class PublicKeyInfo(algorithm: String,
                         created: DateTime,
                         hwDeviceId: String,
                         previousPubKeyId: Option[String] = None,
                         pubKey: String,
                         pubKeyId: String,
                         validNotAfter: Option[DateTime] = None,
                         validNotBefore: DateTime
                        )

case class PublicKeys(publicKeys: Set[PublicKey])
