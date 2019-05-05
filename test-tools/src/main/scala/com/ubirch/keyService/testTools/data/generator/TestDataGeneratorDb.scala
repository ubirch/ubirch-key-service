package com.ubirch.keyService.testTools.data.generator

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.{Hash, Utils}
import com.ubirch.key.model.db.{FindTrusted, FindTrustedSigned, PublicKey, PublicKeyInfo, Revokation, SignedRevoke, SignedTrustRelation, TrustRelation}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil
import org.apache.commons.codec.binary.Hex
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

    val pubKeyIdToUse = Utils.hashToHex(pubKeyToUse, Hash.SHA256)


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
                      infoAlgorithm: String,
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
    val privKey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode(privateKey),
      associateCurve(infoAlgorithm))
    val signature = Base64.getEncoder.encodeToString(privKey.sign(pubKeyInfoString.getBytes))
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

  def signedTrustRelation(from: KeyMaterialDb,
                          to: KeyMaterialDb,
                          trustLevel: Int = 50,
                          validNotAfter: Option[DateTime] = Some(DateUtil.nowUTC.plusMonths(3))
                         ): SignedTrustRelation = {

    val algorithmCurve = associateCurve(from.publicKey.pubKeyInfo.algorithm)

    val trustRelation = TrustRelation(
      created = DateUtil.nowUTC,
      curveAlgorithm = from.publicKey.pubKeyInfo.algorithm,
      sourcePublicKey = from.publicKey.pubKeyInfo.pubKey,
      targetPublicKey = to.publicKey.pubKeyInfo.pubKey,
      trustLevel = trustLevel,
      validNotAfter = validNotAfter
    )
    val trustRelationJson = Json4sUtil.any2String(trustRelation).get
    val privKey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode(from.privateKeyString), algorithmCurve)
    val signature = Base64.getEncoder.encodeToString(privKey.sign(trustRelationJson.getBytes))

    SignedTrustRelation(trustRelation, signature)

  }

  def findTrustedSigned(sourcePublicKey: String,
                        sourcePrivateKey: String,
                        minTrust: Int = 50,
                        depth: Int = 1,
                        algortihmCurve: String
                       ): FindTrustedSigned = {

    val findTrusted = FindTrusted(
      curveAlgorithm = algortihmCurve,
      depth = depth,
      minTrustLevel = minTrust,
      sourcePublicKey = sourcePublicKey
    )
    val payload = Json4sUtil.any2String(findTrusted).get

    val privKeyB64: Array[Byte] = Base64.getDecoder.decode(sourcePrivateKey)
    val privKey = GeneratorKeyFactory.getPrivKey(privKeyB64, associateCurve(algortihmCurve))
    FindTrustedSigned(
      findTrusted = findTrusted,
      signature = Base64.getEncoder.encodeToString(privKey.sign(payload.getBytes))
    )

  }

  def signedRevoke(publicKey: String,
                   privateKey: String,
                   created: DateTime = DateUtil.nowUTC,
                   algorithmCurve: String
                  ): SignedRevoke = {

    val revokation = Revokation(
      publicKey = publicKey,
      revokationDate = created,
      curveAlgorithm = algorithmCurve
    )
    val payload = Json4sUtil.any2String(revokation).get
    val privKey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode(privateKey), associateCurve(algorithmCurve))
    SignedRevoke(
      revokation = revokation,
      signature = Base64.getEncoder.encodeToString(privKey.sign(payload.getBytes))
    )

  }

  def generateOneKeyPair(algorithmCurve: String): KeyMaterialDb = {
    val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(algorithmCurve))

    val (privateKeyA, publicKeyA) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)),
      Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)))

    KeyGenUtil.keyMaterialDb(publicKey = publicKeyA,
      privateKey = privateKeyA,
      algorithmCurve)

  }

  def generateTwoKeyPairs(algortihmCurve: String): KeyMaterialAAndBDb = {

    val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(algortihmCurve))
    val (privateKeyA, publicKeyA) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)),
      Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)))
    val keyMaterialA = KeyGenUtil.keyMaterialDb(publicKey = publicKeyA,
      privateKey = privateKeyA,
      algortihmCurve)

    val privKey2 = GeneratorKeyFactory.getPrivKey(associateCurve(algortihmCurve))
    val (privateKeyB, publicKeyB) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey2.getRawPrivateKey)),
      Base64.getEncoder.encodeToString(Hex.decodeHex(privKey2.getRawPublicKey)))

    val keyMaterialB = KeyGenUtil.keyMaterialDb(publicKey = publicKeyB,
      privateKey = privateKeyB,
      algortihmCurve)

    val publicKeys = Set(
      keyMaterialA.publicKey,
      keyMaterialB.publicKey
    )

    KeyMaterialAAndBDb(
      keyMaterialA = keyMaterialA,
      keyMaterialB = keyMaterialB,
      publicKeys = publicKeys
    )

  }

}

case class KeyMaterialAAndBDb(keyMaterialA: KeyMaterialDb,
                              keyMaterialB: KeyMaterialDb,
                              publicKeys: Set[PublicKey]
                             ) {

  def privateKeyA(): String = keyMaterialA.privateKeyString

}

case class KeyMaterialDb(privateKeyString: String, publicKey: PublicKey)
