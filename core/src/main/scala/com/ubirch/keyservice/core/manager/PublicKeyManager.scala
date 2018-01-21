package com.ubirch.keyservice.core.manager

import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.key.model.db.{Neo4jLabels, PublicKey, PublicKeyInfo}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil
import org.anormcypher._
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-04-27
  */
object PublicKeyManager extends StrictLogging {

  /**
    * Persist a [[PublicKey]].
    *
    * @param pubKey    public key to persist
    * @param neo4jREST Neo4j connection
    * @return persisted public key; None if something went wrong
    */
  def create(pubKey: PublicKey)
            (implicit neo4jREST: Neo4jREST): Future[Option[PublicKey]] = {

    if (PublicKeyUtil.validateSignature(pubKey)) {

      val data = entityToString(pubKey)
      val cypherStr =
        s"""CREATE (pubKey:${Neo4jLabels.PUBLIC_KEY} $data)
           |RETURN pubKey""".stripMargin
      Cypher(
        cypherStr
      ).executeAsync() map {

        case true =>
          Some(pubKey)

        case false =>
          val errMsg = s"failed to create publicKey: ${pubKey.pubKeyInfo.pubKey}"
          logger.error(errMsg)
          throw new Exception(errMsg)

      }

    } else {
      val errMsg = s"invalid signature: ${pubKey.signature}"
      logger.error(errMsg)
      throw new Exception(errMsg)
    }

  }

  /**
    * Gives us a Set of all currently valid public keys for a given hardware id.
    *
    * @param hardwareId      hardware id for which to search for currently valid keys
    * @param neo4jConnection Neo4j connection
    * @return currently valid public keys; empty if none are found
    */
  def currentlyValid(hardwareId: String)
                    (implicit neo4jConnection: Neo4jConnection): Future[Set[PublicKey]] = {

    val now = DateTime.now(DateTimeZone.UTC).toString
    logger.debug(s"now=$now")
    Cypher(
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
    ).async() map { result =>

      logger.debug(s"found ${result.size} results for hardwareId=$hardwareId")
      for (row <- result) {
        logger.debug(s"(hardwareId=$hardwareId) row=$row")
      }

      mapToPublicKey(result)

    } recover {
      throw new Exception("key not found")
    }
  }

  def findByPubKey(pubKey: String)
                  (implicit neo4jConnection: Neo4jConnection): Future[Option[PublicKey]] = {

    // TODO automated tests
    logger.debug(s"findByPubKey($pubKey)")
    Cypher(
      s"""MATCH (pubKey: ${Neo4jLabels.PUBLIC_KEY})
         |WHERE pubKey.infoPubKey = {infoPubKey}
         |RETURN pubKey
       """.stripMargin
    )
      .on("infoPubKey" -> pubKey)
      .async() map { result =>

      logger.debug(s"found ${result.size} results for pubKey=$pubKey")
      for (row <- result) {
        logger.debug(s"(pubKey=$pubKey) row=$row")
      }

      val resultSet = mapToPublicKey(result)
      if (resultSet.isEmpty) {
        None
      } else {
        Some(resultSet.head)
      }

    }

  }

  // TODO implement findByHwDeviceId()

  private def toKeyValueMap(publicKey: PublicKey): Map[String, Any] = {

    var keyValue: Map[String, Any] = Map(
      "infoHwDeviceId" -> publicKey.pubKeyInfo.hwDeviceId,
      "infoPubKey" -> publicKey.pubKeyInfo.pubKey,
      "infoPubKeyId" -> publicKey.pubKeyInfo.pubKeyId,
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

      val previousPublicKeyId = props.getOrElse("infoPreviousPubKeyId", "--UNDEFINED--").asInstanceOf[String] match {
        case "--UNDEFINED--" => None
        case s: String => Some(s)
      }

      val previousPublicKeySignature = props.getOrElse("previousPubKeySignature", "--UNDEFINED--").asInstanceOf[String] match {
        case "--UNDEFINED--" => None
        case s: String => Some(s)
      }

      PublicKey(
        pubKeyInfo = PublicKeyInfo(
          hwDeviceId = props("infoHwDeviceId").asInstanceOf[String],
          pubKey = props("infoPubKey").asInstanceOf[String],
          pubKeyId = props("infoPubKeyId").asInstanceOf[String],
          algorithm = props("infoAlgorithm").asInstanceOf[String],
          previousPubKeyId = previousPublicKeyId,
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
