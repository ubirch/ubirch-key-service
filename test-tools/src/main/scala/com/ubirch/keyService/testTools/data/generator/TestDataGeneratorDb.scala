package com.ubirch.keyService.testTools.data.generator

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.crypto.hash.HashUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo, SignedTrustRelation}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

import org.joda.time.{DateTime, DateTimeZone}

/**
  * author: cvandrei
  * since: 2017-05-09
  */
object TestDataGeneratorDb {

  /**
    * Generate a [[PublicKeyInfo]] with all fields set.
    */
  def publicKeyInfo(hwDeviceId: String = UUIDUtil.uuidStr,
                    pubKey: String,
                    pubKeyId: String = UUIDUtil.uuidStr,
                    algorithm: String = "ed25519-sha-512",
                    previousPubKeyId: Option[String] = None,
                    created: DateTime = DateTime.now(DateTimeZone.UTC),
                    validNotBefore: DateTime = DateTime.now(DateTimeZone.UTC),
                    validNotAfter: Option[DateTime] = None
                   ): PublicKeyInfo = {

    val pubKeyToUse = pubKey

    val pubKeyIdToUse = HashUtil.sha256HexString(pubKeyToUse)


    PublicKeyInfo(
      hwDeviceId = hwDeviceId,
      pubKey = pubKeyToUse,
      pubKeyId = pubKeyIdToUse,
      algorithm = algorithm,
      created = created,
      validNotBefore = validNotBefore,
      validNotAfter = validNotAfter
    )
  }

  /**
    * Generate a [[PublicKeyInfo]] for test purposes with only mandatory fields being set. Fields not set are:
    *
    * <li>
    * <ul>previousPubKeyId</ul>
    * <ul>validNotAfter</ul>
    * </li>
    */
  def publicKeyInfoMandatoryOnly(hwDeviceId: String = UUIDUtil.uuidStr,
                                 pubKey: String,
                                 pubKeyId: String = UUIDUtil.uuidStr,
                                 algorithm: String = "ed25519-sha-512",
                                 created: DateTime = DateTime.now(DateTimeZone.UTC),
                                 validNotBefore: DateTime = DateTime.now(DateTimeZone.UTC)
                                ): PublicKeyInfo = {

    publicKeyInfo(
      hwDeviceId = hwDeviceId,
      pubKey = pubKey,
      pubKeyId = pubKeyId,
      algorithm = algorithm,
      previousPubKeyId = None,
      created = created,
      validNotBefore = validNotBefore,
      validNotAfter = None
    )
  }

  /**
    * Generates a [[PublicKey]] for test purposes. All fields will have values.
    */
  def createPublicKey(privateKey: String,
                      infoPubKey: String,
                      infoHwDeviceId: String = UUIDUtil.uuidStr,
                      infoPubKeyId: String = UUIDUtil.uuidStr,
                      infoAlgorithm: String = "ed25519-sha-512",
                      infoPreviousPubKeyId: Option[String] = None,
                      infoCreated: DateTime = DateTime.now(DateTimeZone.UTC),
                      infoValidNotBefore: DateTime = DateTime.now(DateTimeZone.UTC),
                      infoValidNotAfter: Option[DateTime] = Some(DateTime.now(DateTimeZone.UTC).plusDays(7))
                     ): PublicKey = {

    val pubKeyInfo = publicKeyInfo(
      hwDeviceId = infoHwDeviceId,
      pubKey = infoPubKey,
      pubKeyId = infoPubKeyId,
      algorithm = infoAlgorithm,
      created = infoCreated,
      validNotBefore = infoValidNotBefore,
      validNotAfter = infoValidNotAfter
    )

    val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
    val signature = EccUtil.signPayload(privateKey, pubKeyInfoString)

    PublicKey(
      pubKeyInfo = pubKeyInfo,
      signature = signature
    )

  }

  /**
    * Generates a [[PublicKey]] for test purposes with only mandatory fields being set. Fields not set are:
    *
    * <li>
    * <ul>previousPubKeySignature</ul>
    * <ul>pubKeyInfo.previousPubKeyId</ul>
    * <ul>pubKeyInfo.validNotAfter</ul>
    * </li>
    */
  def publicKeyMandatoryOnly(privateKey: String,
                             infoHwDeviceId: String = UUIDUtil.uuidStr,
                             infoPubKey: String,
                             infoPubKeyId: String = UUIDUtil.uuidStr,
                             infoAlgorithm: String = "ed25519-sha-512",
                             infoCreated: DateTime = DateTime.now(DateTimeZone.UTC),
                             infoValidNotBefore: DateTime = DateTime.now(DateTimeZone.UTC)
                            ): PublicKey = {


    val pubKey = createPublicKey(privateKey = privateKey,
      infoHwDeviceId = infoHwDeviceId,
      infoPubKey = infoPubKey,
      infoPubKeyId = infoPubKeyId,
      infoAlgorithm = infoAlgorithm,
      infoCreated = infoCreated,
      infoValidNotBefore = infoValidNotBefore,
      infoValidNotAfter = None
    )

    pubKey

  }

  def signedTrustRelation(from: KeyMaterial, to: KeyMaterial, trustLevel: Int = 50): SignedTrustRelation = {

    val trustKeyRest = TestDataGeneratorRest.signedTrustRelation(
      from = from,
      to = to,
      trustLevel = trustLevel
    )
    Json4sUtil.any2any[SignedTrustRelation](trustKeyRest)

  }

}
