package com.ubirch.key.model.db

import com.ubirch.util.uuid.UUIDUtil
import org.joda.time.DateTime

/**
  *
  * @param pubKeyInfo
  * @param signature
  * @param previousPubKeySignature
  */
case class PublicKey(pubKeyInfo: PublicKeyInfo,
                     signature: String,
                     previousPubKeySignature: Option[String] = None
                    )

/**
  *
  * @param hwDeviceId       hardware id of device which owns the private key
  * @param pubKey           base 64 encoded public key
  * @param pubKeyId         unique public key id
  * @param algorithm        used asymetric crypto algorithm, default is ed25519-sha-512
  * @param previousPubKeyId unique public key id of previous plublic key
  * @param created          UTC timestamp of PublicKeyInfo object creation
  * @param validNotBefore   UTC timestamp since when PublicKey is valid
  * @param validNotAfter    UTC timestamp until when PublicKey is valid
  */
case class PublicKeyInfo(
                          algorithm: String = "ed25519-sha-512",
                          created: DateTime = new DateTime(),
                          hwDeviceId: String,
                          previousPubKeyId: Option[String] = None,
                          pubKey: String,
                          pubKeyId: String = UUIDUtil.uuidStr,
                          validNotAfter: Option[DateTime] = None,
                          validNotBefore: DateTime = new DateTime()
                        )
