package com.ubirch.keyservice.core.manager.util

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo, Revokation, SignedRevoke, SignedTrustRelation, TrustRelation, TrustedKeyResult}
import com.ubirch.util.neo4j.utils.Neo4jParseUtil

import org.joda.time.DateTime
import org.neo4j.driver.v1.types.{Node, Relationship}
import org.neo4j.driver.v1.{Record, Value}

import scala.language.postfixOps
import scala.collection.JavaConverters._

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

  private def nodeToPublicKey(node: Node): PublicKey = {

    val pubKeyInfo = PublicKeyInfo(
      hwDeviceId = node.get("infoHwDeviceId").asString(),
      pubKey = node.get("infoPubKey").asString(),
      pubKeyId = node.get("infoPubKeyId").asString("--UNDEFINED--"),
      algorithm = node.get("infoAlgorithm").asString(),
      previousPubKeyId = if (node.containsKey("infoPreviousPubKeyId")) {
        Some(node.get("infoPreviousPubKeyId").asString)
      } else None,
      created = DateTime.parse(node.get("infoCreated").asString()),
      validNotBefore = DateTime.parse(node.get("infoValidNotBefore").asString),
      validNotAfter = if (node.containsKey("infoValidNotAfter")) {
        Some(DateTime.parse(node.get("infoValidNotAfter").asString()))
      } else None
    )

    PublicKey(
      pubKeyInfo = pubKeyInfo,
      signature = node.get("signature").asString(),
      previousPubKeySignature = if (node.containsKey("previousPubKeySignature")) {
        Some(node.get("previousPubKeySignature").asString())
      } else None,
      raw = if (node.containsKey("raw")) {
        Some(node.get("raw").asString())
      } else None,
      signedRevoke = signedRevokeFromNode(node)
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



  private def relationshipToSignedTrust(relationship: Relationship): SignedTrustRelation = {

    val trustRelation = TrustRelation(
      created = DateTime.parse(relationship.get("trustCreated").asString()),
      sourcePublicKey = relationship.get("trustSource").asString,
      targetPublicKey = relationship.get("trustTarget").asString(),
      trustLevel = relationship.get("trustLevel").asInt,
      validNotAfter = if (relationship.containsKey("trustNotValidAfter")) {
        Some(DateTime.parse(relationship.get("trustNotValidAfter").asString))
      } else None
    )

    SignedTrustRelation(
      trustRelation = trustRelation,
      signature = relationship.get("signature").asString,
      created = DateTime.parse(relationship.get("created").asString)
    )

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

  def recordsToTrustPaths(records: Seq[Record], pathKey: String): Seq[Seq[TrustPath]] = {

    records map { record =>

      val values = record.get(pathKey)
      val path = values.asPath()
      val start = path.start()
      val relationships = path.relationships().asScala
      val end = path.end()
      val pathLength = path.length()
      val nodes = path.nodes().asScala

      val pubKeyNodes = nodes.toSeq map { node =>
        nodeToPublicKey(node)
      }
      val trustRelationships = relationships.toSeq map { relationship =>
        relationshipToSignedTrust(relationship)
      }

      require(pubKeyNodes.size == trustRelationships.size + 1, "there has to be one more node than there are relationships")
      trustRelationships.zipWithIndex.map {

        case (signedTrust, index) =>

          TrustPath(
            from = pubKeyNodes(index),
            signedTrust = signedTrust,
            to = pubKeyNodes(index + 1)
          )

      }

    }

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

  private def signedRevokeFromNode(node: Node): Option[SignedRevoke] = {

    val revokeSignature = node.get("revokeSignature").asString("--UNDEFINED--")
    val revokePublicKey = node.get("revokePublicKey").asString("--UNDEFINED--")
    val revokeDate = if (node.containsKey("revokeDateTime")) {
      Some(DateTime.parse(node.get("revokeDateTime").asString()))
    } else None
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

case class TrustPath(from: PublicKey,
                     signedTrust: SignedTrustRelation,
                     to: PublicKey)
