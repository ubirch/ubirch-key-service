package com.ubirch.keyService.testTools.data.generator

import com.ubirch.crypto.hash.HashUtil
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo}
import com.ubirch.util.uuid.UUIDUtil

import org.joda.time.{DateTime, DateTimeZone}

/**
  * author: cvandrei
  * since: 2017-05-09
  */
object TestDataGenerator {

  def publicKeyInfo(hwDeviceId: String = UUIDUtil.uuidStr,
                    pubKey: Option[String] = None,
                    pubKeyId: Option[String] = None,
                    algorithm: String = "RSA4096",
                    previousPubKeyId: Option[String] = None,
                    created: DateTime = DateTime.now(DateTimeZone.UTC),
                    validNotBefore: DateTime = DateTime.now(DateTimeZone.UTC),
                    validNotAfter: Option[DateTime] = None
                   ): PublicKeyInfo = {

    val pubKeyToUse = pubKey match {
      case None => s"some-public-key-$hwDeviceId"
      case Some(s) => s
    }

    val pubKeyIdToUse = pubKeyId match {
      case None => HashUtil.sha256HexString(pubKeyToUse)
      case Some(s) => s
    }

    PublicKeyInfo(
      hwDeviceId = hwDeviceId,
      pubKey = pubKeyToUse,
      pubKeyId = pubKeyIdToUse,
      algorithm = algorithm,
      previousPubKeyId = previousPubKeyId,
      created = created,
      validNotBefore = validNotBefore,
      validNotAfter = validNotAfter
    )

  }

  /**
    * Generates a [[PublicKey]] for test purposes. All fields will have values unless stated otherwise in the param doc.
    *
    * @param previousPubKeySignature optional field: None if not specified
    * @param infoPreviousPubKeyId optional field: None if not specified
    * @param infoValidNotAfter optional field: None if not specified
    * @return
    */
  def publicKey(signature: String = "some-signature",
                previousPubKeySignature: Option[String] = None,
                infoHwDeviceId: String = UUIDUtil.uuidStr,
                infoPubKey: Option[String] = None,
                infoPubKeyId: Option[String] = None,
                infoAlgorithm: String = "RSA4096",
                infoPreviousPubKeyId: Option[String] = None,
                infoCreated: DateTime = DateTime.now(DateTimeZone.UTC),
                infoValidNotBefore: DateTime = DateTime.now(DateTimeZone.UTC),
                infoValidNotAfter: Option[DateTime] = None
               ): PublicKey = {

    val pubKeyInfo = publicKeyInfo(
      hwDeviceId = infoHwDeviceId,
      pubKey = infoPubKey,
      pubKeyId = infoPubKeyId,
      algorithm = infoAlgorithm,
      previousPubKeyId = infoPreviousPubKeyId,
      created = infoCreated,
      validNotBefore = infoValidNotBefore,
      validNotAfter = infoValidNotAfter
    )

    PublicKey(
      pubKeyInfo = pubKeyInfo,
      signature = signature,
      previousPubKeySignature = previousPubKeySignature
    )

  }

}
