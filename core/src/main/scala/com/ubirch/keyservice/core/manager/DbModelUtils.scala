package com.ubirch.keyservice.core.manager

import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo, SignedTrustRelation, TrustRelation, TrustedKeyResult}
import com.ubirch.util.neo4j.utils.Neo4jParseUtil

import org.neo4j.driver.v1.{Record, Value}

import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2018-09-14
  */
object DbModelUtils {

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

    keyValue

  }

  def publicKeyToString(publicKey: PublicKey): String = {
    val keyValue = publicKeyToKeyValueMap(publicKey)
    Neo4jParseUtil.keyValueToString(keyValue)
  }

  def recordsToPublicKeys(records: Seq[Record], recordLabel: String): Set[PublicKey] = {

    records map { record =>

      val pubKey = record.get(recordLabel)
      parsePublicKeyFromRecord(pubKey)

    } toSet

  }

  private def parsePublicKeyFromRecord(pubKey: Value) = {
    PublicKey(
      pubKeyInfo = PublicKeyInfo(
        hwDeviceId = Neo4jParseUtil.asType[String](pubKey, "infoHwDeviceId"),
        pubKey = Neo4jParseUtil.asType[String](pubKey, "infoPubKey"),
        pubKeyId = Neo4jParseUtil.asTypeOrDefault[String](pubKey, "infoPubKeyId", "--UNDEFINED--"),
        algorithm = Neo4jParseUtil.asType[String](pubKey, "infoAlgorithm"),
        previousPubKeyId = Neo4jParseUtil.asTypeOption[String](pubKey, "infoPreviousPubKeyId"),
        created = Neo4jParseUtil.asDateTime(pubKey, "infoCreated"),
        validNotBefore = Neo4jParseUtil.asDateTime(pubKey, "infoValidNotBefore"),
        validNotAfter = Neo4jParseUtil.asDateTimeOption(pubKey, "infoValidNotAfter")
      ),
      signature = Neo4jParseUtil.asType(pubKey, "signature"),
      previousPubKeySignature = Neo4jParseUtil.asTypeOption[String](pubKey, "previousPubKeySignature"),
      raw = Neo4jParseUtil.asTypeOption[String](pubKey, "raw")
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

}
