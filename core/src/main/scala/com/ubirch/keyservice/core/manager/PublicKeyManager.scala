package com.ubirch.keyservice.core.manager

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.db.Neo4jLabels
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo}

import org.anormcypher.{Cypher, CypherResultRow, Neo4jConnection, NeoNode}
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-04-27
  */
object PublicKeyManager extends StrictLogging {

  def create(toCreate: PublicKey)
            (implicit neo4jConnection: Neo4jConnection): Future[Option[PublicKey]] = {

    // TODO automated tests
    // TODO verify that toCreate.signature matches toCreate.pubkeyInfo
    val data = entityToString(toCreate)

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
    val now = DateTime.now(DateTimeZone.UTC).toString
    logger.debug(s"now=$now")
    val query = Cypher(
      s"""MATCH (pubKey: ${Neo4jLabels.PUBLIC_KEY}  {infoHwDeviceId: {hwDeviceId}})
         |WHERE
         |  {now} > pubKey.infoValidNotBefore
         |  AND (
         |    pubKey.infoValidNotAfter is null
         |     OR {now} < pubKey.infoValidNotAfter
         |  )
         |RETURN pubKey
       """.stripMargin
    ).on(
      "hwDeviceId" -> hardwareId,
      "now" -> now
    )
    val result: Seq[CypherResultRow] = query()

    logger.debug(s"found ${result.size} results for hardwareId=$hardwareId")
    for (row <- result) {
      logger.debug(s"(hardwareId=$hardwareId) row=$row")
    }

    val publicKeys: Set[PublicKey] = mapToPublicKey(result)

    Future(publicKeys)

  }

  private def toKeyValueMap(publicKey: PublicKey): Map[String, Any] = {

    var keyValue: Map[String, Any] = Map(
      "infoHwDeviceId" -> publicKey.pubkeyInfo.hwDeviceId,
      "infoPubKey" -> publicKey.pubkeyInfo.pubKey,
      "infoAlgorithm" -> publicKey.pubkeyInfo.algorithm,
      "infoCreated" -> publicKey.pubkeyInfo.created,
      "infoValidNotBefore" -> publicKey.pubkeyInfo.validNotBefore,
      "signature" -> publicKey.signature
    )
    if (publicKey.pubkeyInfo.validNotAfter.isDefined) {
      keyValue += "infoValidNotAfter" -> publicKey.pubkeyInfo.validNotAfter.get
    }
    if (publicKey.pubkeyInfo.previousPubKey.isDefined) {
      keyValue += "infoPreviousPubKey" -> publicKey.pubkeyInfo.previousPubKey.get
    }
    if (publicKey.previousPubKeySignature.isDefined) {
      keyValue += "previousPubKeySignature" -> publicKey.previousPubKeySignature.get
    }

    keyValue

  }

  private def keyValueToString(keyValue: Map[String, Any]): String = {

    val data: String = keyValue map {
      case (key, value: Int) => s"""$key: $value"""
      case (key, value: Long) => s"""$key: $value"""
      case (key, value: Boolean) => s"""$key: $value"""
      case (key, value: String) => s"""$key: "$value""""
      case (key, value) => s"""$key: "$value""""
    } mkString("{", ", ", "}")
    logger.debug(s"keyValues.string -- $data")

    data

  }

  private def entityToString(publicKey: PublicKey): String = {
    val keyValue = toKeyValueMap(publicKey)
    keyValueToString(keyValue)
  }

  private def mapToPublicKey(result: Seq[CypherResultRow]): Set[PublicKey] = {

    result map { row =>

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

    } toSet

  }

}
