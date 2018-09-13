package com.ubirch.keyService.testTools.data.generator

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.crypto.hash.HashUtil
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo, SignedTrustRelation, TrustRelation}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Random

/**
  * author: cvandrei
  * since: 2017-05-09
  */
object TestDataGeneratorRest {

  /**
    * Generate a [[PublicKeyInfo]] with all fields set.
    */
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
      case None => s"some-public-key-$hwDeviceId-${Random.nextLong()}"
      case Some(s) => s
    }

    val pubKeyIdToUse = pubKeyId match {
      case None => HashUtil.sha256HexString(pubKeyToUse)
      case Some(s) => s
    }

    val previousPubKeyToUse = pubKey match {
      case None => s"previous-public-key-$hwDeviceId-${Random.nextLong()}"
      case Some(s) => s
    }

    val previousPubKeyIdToUse = pubKeyId match {
      case None => Some(HashUtil.sha256HexString(previousPubKeyToUse))
      case Some(s) => Some(s)
    }

    PublicKeyInfo(
      algorithm = algorithm,
      created = created,
      hwDeviceId = hwDeviceId,
      previousPubKeyId = previousPubKeyIdToUse,
      pubKey = pubKeyToUse,
      pubKeyId = pubKeyIdToUse,
      validNotAfter = validNotAfter,
      validNotBefore = validNotBefore
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
                                 pubKey: Option[String] = None,
                                 algorithm: String = "RSA4096",
                                 created: DateTime = DateTime.now(DateTimeZone.UTC),
                                 validNotBefore: DateTime = DateTime.now(DateTimeZone.UTC)
                                ): PublicKeyInfo = {

    publicKeyInfo(
      hwDeviceId = hwDeviceId,
      pubKey = pubKey,
      algorithm = algorithm,
      previousPubKeyId = None,
      created = created,
      validNotBefore = validNotBefore,
      validNotAfter = None
    ).copy(previousPubKeyId = None, validNotAfter = None)

  }

  /**
    * Generates a [[PublicKey]] for test purposes. All fields will have values.
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
                infoValidNotAfter: Option[DateTime] = Some(DateTime.now(DateTimeZone.UTC).plusDays(7))
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

    val previousPubKeySigToUse = Some(s"previous-pub-key-signature-${UUIDUtil.uuidStr}")
    PublicKey(
      pubKeyInfo = pubKeyInfo,
      signature = signature,
      previousPubKeySignature = previousPubKeySigToUse
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
  def publicKeyMandatoryOnly(signature: String = "some-signature",
                             infoHwDeviceId: String = UUIDUtil.uuidStr,
                             infoPubKey: Option[String] = None,
                             infoPubKeyId: Option[String] = None,
                             infoAlgorithm: String = "RSA4096",
                             infoCreated: DateTime = DateTime.now(DateTimeZone.UTC),
                             infoValidNotBefore: DateTime = DateTime.now(DateTimeZone.UTC)
                            ): PublicKey = {

    val pubKey = publicKey(
      signature = signature,
      infoHwDeviceId = infoHwDeviceId,
      infoPubKey = infoPubKey,
      infoPubKeyId = infoPubKeyId,
      infoAlgorithm = infoAlgorithm,
      infoCreated = infoCreated,
      infoValidNotBefore = infoValidNotBefore
    )

    val info = pubKey.pubKeyInfo.copy(previousPubKeyId = None, validNotAfter = None)

    pubKey.copy(previousPubKeySignature = None, pubKeyInfo = info)

  }

  def signedTrustRelation(from: KeyMaterial, to: KeyMaterial, trustLevel: Int = 50): SignedTrustRelation = {

    val trustRelation = TrustRelation(
      created = DateUtil.nowUTC,
      sourcePublicKey = from.publicKey.pubKeyInfo.pubKey,
      targetPublicKey = to.publicKey.pubKeyInfo.pubKey,
      trustLevel = trustLevel,
      validNotAfter = Some(DateUtil.nowUTC.plusMonths(3))
    )
    val trustRelationJson = Json4sUtil.any2String(trustRelation).get
    val signature = EccUtil.signPayload(from.privateKeyString, trustRelationJson)

    SignedTrustRelation(trustRelation, signature)

  }

  def generateTwoKeyPairs(): KeyMaterialAAndBRest = {

    val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
    val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

    val publicKeys = Set(
      Json4sUtil.any2any[PublicKey](keyMaterialA.publicKey),
      Json4sUtil.any2any[PublicKey](keyMaterialB.publicKey)
    )

    KeyMaterialAAndBRest(
      keyMaterialA = keyMaterialA,
      keyMaterialB = keyMaterialB,
      publicKeys = publicKeys
    )

  }

}

case class KeyMaterial(privateKeyString: String, publicKey: PublicKey)

case class KeyMaterialAAndBRest(keyMaterialA: KeyMaterial,
                            keyMaterialB: KeyMaterial,
                            publicKeys: Set[PublicKey]
                           ) {

  def privateKeyA(): String = keyMaterialA.privateKeyString

}
