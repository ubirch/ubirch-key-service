package com.ubirch.keyservice.core.manager

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.db.Neo4jLabels
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo}

import org.anormcypher.{Cypher, Neo4jConnection, NeoNode}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-04-27
  */
object PublicKeyManager extends StrictLogging {

  def create(toCreate: PublicKey)
            (implicit neo4jConnection: Neo4jConnection): Future[Option[PublicKey]] = {

    // TODO automated tests
    var keyValue: Map[String, Any] = Map(
      "infoHwDeviceId" -> toCreate.pubkeyInfo.hwDeviceId,
      "infoPubKey" -> toCreate.pubkeyInfo.pubKey,
      "infoAlgorithm" -> toCreate.pubkeyInfo.algorithm,
      "infoCreated" -> toCreate.pubkeyInfo.created,
      "infoValidNotBefore" -> toCreate.pubkeyInfo.validNotBefore,
      "signature" -> toCreate.signature
    )
    if (toCreate.pubkeyInfo.validNotAfter.isDefined) {
      keyValue += "infoValidNotAfter" -> toCreate.pubkeyInfo.validNotAfter.get
    }
    if (toCreate.pubkeyInfo.previousPubKey.isDefined) {
      keyValue += "infoPreviousPubKey" -> toCreate.pubkeyInfo.previousPubKey.get
    }
    if (toCreate.previousPubKeySignature.isDefined) {
      keyValue += "previousPubKeySignature" -> toCreate.previousPubKeySignature.get
    }

    val data: String = (
      keyValue map {
        case (key, value: Int) => s"""$key: $value"""
        case (key, value: Long) => s"""$key: $value"""
        case (key, value: Boolean) => s"""$key: $value"""
        case (key, value: String) => s"""$key: "$value""""
        case (key, value) => s"""$key: "$value""""
      }
        mkString("{", ", ", "}")
      )
    logger.debug(s"keyValues.string -- $data")

    val result = Cypher(
      s"""CREATE (pubKey:${Neo4jLabels.PUBLIC_KEY} $data)
         |RETURN pubKey""".stripMargin
    ).execute()

    // TODO Future doesn't seem to be necessary...remove?
    if (!result) {
      logger.error(s"failed to create public key: publicKey=$toCreate")
      Future(None)
    } else {
      Future(Some(toCreate))
    }

  }

  def currentlyValid(hardwareId: String)
                    (implicit neo4jConnection: Neo4jConnection): Future[Set[PublicKey]] = {

    // TODO automated tests
    val result = Cypher(
      s"""MATCH (pubKey: ${Neo4jLabels.PUBLIC_KEY})
         |WHERE pubKey.infoHwDeviceId = {hwDeviceId}
         |RETURN pubKey
       """.stripMargin
    ).on("hwDeviceId" -> hardwareId)()

    logger.debug(s"found ${result.size} results for hardwareId=$hardwareId")
    for (foo <- result) {
      logger.debug(s"(hardwareId=$hardwareId) row=$foo")
    }

    val publicKeys: Seq[PublicKey] = result map { row =>

      val props = row[NeoNode]("pubKey").props

      val validNotAfter = props.getOrElse("infoValidNotAfter", "--UNDEFINED--").asInstanceOf[String] match {
        case "--UNDEFINED--" => None
        case dateTimeString: String => Some(DateTime.parse(dateTimeString))
      }

      val previousPublicKey = props.getOrElse("infoPreviousPubKey", "--UNDEFINED--").asInstanceOf[String] match {
        case "--UNDEFINED--" => None
        case s: String => Some(s)
      }

      val previousPublicKeySignature = props.getOrElse("previousPubKeySignature", "--UNDEFINED--").asInstanceOf[String] match {
        case "--UNDEFINED--" => None
        case s: String => Some(s)
      }

      PublicKey(
        pubkeyInfo = PublicKeyInfo(
          hwDeviceId = props("infoHwDeviceId").asInstanceOf[String],
          pubKey = props("infoPubKey").asInstanceOf[String],
          algorithm = props("infoAlgorithm").asInstanceOf[String],
          previousPubKey = previousPublicKey,
          created = DateTime.parse(props("infoCreated").asInstanceOf[String]),
          validNotBefore = DateTime.parse(props("infoValidNotBefore").asInstanceOf[String]),
          validNotAfter = validNotAfter
        ),
        signature = props("signature").asInstanceOf[String],
        previousPubKeySignature = previousPublicKeySignature
      )

    }

    Future(publicKeys.toSet)

  }

}
