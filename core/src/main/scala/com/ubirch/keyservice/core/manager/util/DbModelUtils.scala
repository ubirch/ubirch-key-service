package com.ubirch.keyservice.core.manager.util

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo, Revokation, SignedRevoke, SignedTrustRelation, TrustRelation, TrustedKeyResult}
import com.ubirch.util.neo4j.utils.Neo4jParseUtil

import org.neo4j.driver.v1.{Record, Value}

import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2018-09-14
  */
object DbModelUtils extends StrictLogging {

  def publicKeyToKeyValueMap(publicKey: PublicKey): Map[String, Any] = {

    var keyValue: Map[String, Any] = Map(
      "infoHwDeviceId" -> publicKey.pubKeyInfo.hwDeviceId,
      "infoPubKeyId" -> publicKey.pubKeyInfo.pubKeyId,
      "infoPubKey" -> publicKey.pubKeyInfo.pubKey,
      "infoAlgorithm" -> publicKey.pubKeyInfo.algorithm,
      "infoCreated" -> publicKey.pubKeyInfo.created,
      "infoValidNotBefore" -> publicKey.pubKeyInfo.validNotBefore,
      "signature" -> publicKey.signature
    )
    if (publicKey.pubKeyInfo.validNotAfter.isDefined) {
      keyValue += "infoValidNotAfter" -> publicKey.pubKeyInfo.validNotAfter.get
    }
    if (publicKey.pubKeyInfo.previousPubKeyId.isDefined) {
      keyValue += "infoPreviousPubKeyId" -> publicKey.pubKeyInfo.previousPubKeyId.get
    }
    if (publicKey.previousPubKeySignature.isDefined) {
      keyValue += "previousPubKeySignature" -> publicKey.previousPubKeySignature.get
    }
    if (publicKey.raw.isDefined) {
      keyValue += "raw" -> publicKey.raw.get
    }

    if (publicKey.signedRevoke.isDefined) {
      val signedRevoke = publicKey.signedRevoke.get
      keyValue += "revokeSignature" -> signedRevoke.signature
      keyValue += "revokePublicKey" -> signedRevoke.revokation.publicKey
      keyValue += "revokeDateTime" -> signedRevoke.revokation.revokationDate
    }

    keyValue

  }

  def publicKeyToString(publicKey: PublicKey): String = {
    val keyValue = publicKeyToKeyValueMap(publicKey)
    Neo4jParseUtil.keyValueToString(keyValue)
  }

  def publicKeyToStringSET(publicKey: PublicKey, keyPrefix: String): String = {
    val keyValue = publicKeyToKeyValueMap(publicKey)
    Neo4jParseUtil.keyValueToStringSET(keyValue, keyPrefix)
  }

  def recordsToPublicKeys(records: Seq[Record], recordLabel: String): Set[PublicKey] = {

    records map { record =>

      val pubKey = record.get(recordLabel)
      parsePublicKeyFromRecord(pubKey)

    } toSet

  }

  private def parsePublicKeyFromRecord(pubKey: Value) = {

    val pubKeyInfo = PublicKeyInfo(
      hwDeviceId = Neo4jParseUtil.asType[String](pubKey, "infoHwDeviceId"),
      pubKey = Neo4jParseUtil.asType[String](pubKey, "infoPubKey"),
      pubKeyId = Neo4jParseUtil.asTypeOrDefault[String](pubKey, "infoPubKeyId", "--UNDEFINED--"),
      algorithm = Neo4jParseUtil.asType[String](pubKey, "infoAlgorithm"),
      previousPubKeyId = Neo4jParseUtil.asTypeOption[String](pubKey, "infoPreviousPubKeyId"),
      created = Neo4jParseUtil.asDateTime(pubKey, "infoCreated"),
      validNotBefore = Neo4jParseUtil.asDateTime(pubKey, "infoValidNotBefore"),
      validNotAfter = Neo4jParseUtil.asDateTimeOption(pubKey, "infoValidNotAfter")
    )

    PublicKey(
      pubKeyInfo = pubKeyInfo,
      signature = Neo4jParseUtil.asType(pubKey, "signature"),
      previousPubKeySignature = Neo4jParseUtil.asTypeOption[String](pubKey, "previousPubKeySignature"),
      raw = Neo4jParseUtil.asTypeOption[String](pubKey, "raw"),
      signedRevoke = signedRevokeFromPublicKey(pubKey)
    )

  }

  private def signedTrustRelationToKeyValueMap(signedTrustRelation: SignedTrustRelation): Map[String, Any] = {

    var keyValue: Map[String, Any] = Map(
      "signature" -> signedTrustRelation.signature,
      "created" -> signedTrustRelation.created,
      "trustCreated" -> signedTrustRelation.trustRelation.created,
      "trustSource" -> signedTrustRelation.trustRelation.sourcePublicKey,
      "trustTarget" -> signedTrustRelation.trustRelation.targetPublicKey,
      "trustLevel" -> signedTrustRelation.trustRelation.trustLevel
    )
    if (signedTrustRelation.trustRelation.validNotAfter.isDefined) {
      keyValue += "trustNotValidAfter" -> signedTrustRelation.trustRelation.validNotAfter.get
    }

    keyValue

  }

  def signedTrustRelationToString(signedTrustRelation: SignedTrustRelation): String = {
    val keyValue = signedTrustRelationToKeyValueMap(signedTrustRelation)
    Neo4jParseUtil.keyValueToString(keyValue)
  }

  def recordsToSignedTrustRelationship(records: Seq[Record], recordLabel: String): Set[SignedTrustRelation] = {

    records map { record =>

      val trustRelation = record.get(recordLabel)

      SignedTrustRelation(
        trustRelation = TrustRelation(
          created = Neo4jParseUtil.asDateTime(trustRelation, "trustCreated"),
          sourcePublicKey = Neo4jParseUtil.asType[String](trustRelation, "trustSource"),
          targetPublicKey = Neo4jParseUtil.asType[String](trustRelation, "trustTarget"),
          trustLevel = Neo4jParseUtil.asType[Long](trustRelation, "trustLevel").toInt,
          validNotAfter = Neo4jParseUtil.asDateTimeOption(trustRelation, "trustNotValidAfter")
        ),
        signature = Neo4jParseUtil.asType[String](trustRelation, "signature"),
        created = Neo4jParseUtil.asDateTime(trustRelation, "created")
      )

    } toSet

  }



  def recordsToTrustedKeyResult(records: Seq[Record],
                                depth: Int,
                                publicKeyLabel: String,
                                trustLevelLabel: String
                               ): Set[TrustedKeyResult] = {

    records map { record =>

      val pubKey = record.get(publicKeyLabel)
      val trustLevel = record.get(trustLevelLabel)
      trustLevel.asInt()

      TrustedKeyResult(
        depth = depth,
        trustLevel = trustLevel.asInt(),
        publicKey = parsePublicKeyFromRecord(pubKey)
      )

    } toSet

  }

  private def signedRevokeFromPublicKey(pubKey: Value): Option[SignedRevoke] = {

    val revokeSignature = Neo4jParseUtil.asTypeOrDefault[String](pubKey, "revokeSignature", "--UNDEFINED--")
    val revokePublicKey = Neo4jParseUtil.asTypeOrDefault[String](pubKey, "revokePublicKey", "--UNDEFINED--")
    val revokeDate = Neo4jParseUtil.asDateTimeOption(pubKey, "revokeDateTime")
    if (revokeSignature == "--UNDEFINED--" && revokePublicKey == "--UNDEFINED--" && revokeDate.isEmpty) {

      None

    } else if (revokeSignature != "--UNDEFINED--" && revokePublicKey != "--UNDEFINED--" && revokeDate.isDefined) {

      Some(
        SignedRevoke(
          revokation = Revokation(publicKey = revokePublicKey, revokationDate = revokeDate.get),
          signature = revokeSignature
        )
      )

    } else {

      logger.error(s"public key record in database seems to be invalid: signedRevoke.signature=$revokeSignature, signedRevoke.revokation.publicKey=$revokePublicKey, signedRevoke.revokation.revokationDate=$revokeDate")
      None

    }

  }

}
