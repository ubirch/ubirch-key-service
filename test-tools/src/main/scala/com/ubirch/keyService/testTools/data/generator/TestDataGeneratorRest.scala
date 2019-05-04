package com.ubirch.keyService.testTools.data.generator

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.{Hash, Utils}
import com.ubirch.key.model.rest.{FindTrusted, FindTrustedSigned, PublicKey, PublicKeyInfo, Revokation, SignedRevoke, SignedTrustRelation, TrustRelation}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil
import org.apache.commons.codec.binary.Hex
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
      case None => Utils.hashToHex(pubKeyToUse, Hash.SHA256)
      case Some(s) => s
    }

    val previousPubKeyToUse = pubKey match {
      case None => s"previous-public-key-$hwDeviceId-${Random.nextLong()}"
      case Some(s) => s
    }

    val previousPubKeyIdToUse = pubKeyId match {
      case None => Some(Utils.hashToHex(previousPubKeyToUse, Hash.SHA256))
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

  def signedTrustRelation(from: KeyMaterial,
                          to: KeyMaterial,
                          trustLevel: Int = 50,
                          validNotAfter: Option[DateTime] = Some(DateUtil.nowUTC.plusMonths(3))
                         ): SignedTrustRelation = {

    val trustRelation = TrustRelation(
      created = DateUtil.nowUTC,
      curveAlgorithm = from.publicKey.pubKeyInfo.algorithm,
      sourcePublicKey = from.publicKey.pubKeyInfo.pubKey,
      targetPublicKey = to.publicKey.pubKeyInfo.pubKey,
      trustLevel = trustLevel,
      validNotAfter = validNotAfter
    )
    val trustRelationJson = Json4sUtil.any2String(trustRelation).get
    val privKey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode(from.privateKeyString),
      associateCurve(from.publicKey.pubKeyInfo.algorithm))
    val signature = Base64.getEncoder.encodeToString(privKey.sign(trustRelationJson.getBytes()))

    SignedTrustRelation(trustRelation, signature)

  }

  def findTrustedSigned(depth: Int = 1,
                        sourcePublicKey: String,
                        sourcePrivateKey: String,
                        minTrust: Int = 50,
                        infoAlgorithm: String
                       ): FindTrustedSigned = {

    val findTrusted = FindTrusted(
      depth = depth,
      sourcePublicKey = sourcePublicKey,
      queryDate = DateUtil.nowUTC,
      minTrustLevel = minTrust,
      curveAlgorithm = infoAlgorithm
    )
    val payload = Json4sUtil.any2String(findTrusted).get

    val privKey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode(sourcePrivateKey), associateCurve(infoAlgorithm))
    val signature = Base64.getEncoder.encodeToString(privKey.sign(payload.getBytes))
    FindTrustedSigned(
      findTrusted = findTrusted,
      signature = signature
    )

  }

  def signedRevoke(publicKey: String,
                   privateKey: String,
                   created: DateTime = DateUtil.nowUTC,
                   curveAlgorithm: String
                  ): SignedRevoke = {

    val revokation = Revokation(
      curveAlgorithm = curveAlgorithm,
      publicKey = publicKey,
      revokationDate = created
    )
    val payload = Json4sUtil.any2String(revokation).get

    val privKey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode(privateKey), associateCurve(curveAlgorithm))
    val sign = Base64.getEncoder.encodeToString(privKey.sign(payload.getBytes))
    SignedRevoke(
      revokation = revokation,
      signature = sign
    )

  }

  def generateOneKeyPair(curveAlgorithm: String): KeyMaterial = {

    val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
    val (publicKeyA, privateKeyA) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
      Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
    KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA, algorithmCurve = curveAlgorithm)

  }

  def generateTwoKeyPairs(curveAlgorithm: String): KeyMaterialAAndBRest = {

    val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
    val (publicKeyA, privateKeyA) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
      Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA, algorithmCurve = curveAlgorithm)

    val privKeyB = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
    val (publicKeyB, privateKeyB) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKeyB.getRawPublicKey)),
      Base64.getEncoder.encodeToString(Hex.decodeHex(privKeyB.getRawPrivateKey)))
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB, algorithmCurve = curveAlgorithm)

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
