package com.ubirch.keyservice.core.manager

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyDelete, PublicKeyInfo}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil
import com.ubirch.util.neo4j.utils.Neo4jParseUtil

import org.joda.time.{DateTime, DateTimeZone}
import org.neo4j.driver.v1.Values.parameters
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException
import org.neo4j.driver.v1.{Driver, Record, Transaction, TransactionWork}

import scala.collection.JavaConversions._
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
    * @param neo4jDriver Neo4j connection
    * @return persisted public key; None if something went wrong
    */
  def create(pubKey: PublicKey)
            (implicit neo4jDriver: Driver): Future[Either[Exception, Option[PublicKey]]] = {

    findByPubKey(pubKey.pubKeyInfo.pubKey) flatMap {

      case Some(pk) =>

        val errMsg = s"unable to create publicKey if it already exists: ${pubKey.pubKeyInfo.pubKey}"
        logger.error(errMsg)
        Future(Left(new Exception(errMsg)))

      case None =>

        if (PublicKeyUtil.validateSignature(pubKey)) {

          val data = entityToString(pubKey)
          val query = s"""CREATE (pubKey:PublicKey $data)
               |RETURN pubKey""".stripMargin

          val createResult = try {

            val session = neo4jDriver.session
            try {

              // TODO refactor: readTransactionAsync()
              session.writeTransaction(new TransactionWork[Either[Exception, Option[PublicKey]]]() {
                def execute(tx: Transaction): Either[Exception, Option[PublicKey]] = {

                  val result = tx.run(query)
                  val records = result.list().toSeq
                  logger.debug(s"found ${records.size} results for pubKey=$pubKey")

                  Right(recordsToPublicKeys(records, "pubKey").headOption)

                }
              })

            } finally if (session != null) session.close()

          } catch {

            case su: ServiceUnavailableException =>

              logger.error(s"create() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
              Left(new Exception(s"failed to create publicKey: ${pubKey.pubKeyInfo.pubKey}"))

            case e: Exception =>

              logger.error(s"create() -- Exception: e.message=${e.getMessage}", e)
              Left(new Exception(s"failed to create publicKey: ${pubKey.pubKeyInfo.pubKey}"))

            case re: RuntimeException =>

              logger.error(s"create() -- RuntimeException: re.message=${re.getMessage}", re)
              Left(new Exception(s"failed to create publicKey: ${pubKey.pubKeyInfo.pubKey}"))

          }

          Future(createResult)

          /*
          Cypher(
            cypherStr
          ).executeAsync() map {

            case true =>

              Right(Some(pubKey))

            case false =>

              val errMsg = s"failed to create publicKey: ${pubKey.pubKeyInfo.pubKey}"
              logger.error(errMsg)
              Left(new Exception(errMsg))

          }
          */

        } else {

          val errMsg = s"unable to create public key if signature is invalid: ${pubKey.signature}"
          logger.error(errMsg)
          Future(Left(new Exception(errMsg)))

        }

    }

  }

  /**
    * Gives us a Set of all currently valid public keys for a given hardware id.
    *
    * @param hardwareId  hardware id for which to search for currently valid keys
    * @param neo4jDriver Neo4j connection
    * @return currently valid public keys; empty if none are found
    */
  def currentlyValid(hardwareId: String)
                    (implicit neo4jDriver: Driver): Future[Set[PublicKey]] = {

    val now = DateTime.now(DateTimeZone.UTC).toString
    logger.debug(s"currentlyValid() -- now=$now, hardwareId=$hardwareId")

    val query =
      """MATCH (pubKey: PublicKey {infoHwDeviceId: $hwDeviceId})
        |WHERE
        |  $now > pubKey.infoValidNotBefore
        |  AND (
        |    pubKey.infoValidNotAfter is null
        |     OR $now < pubKey.infoValidNotAfter
        |  )
        |RETURN pubKey
      """.stripMargin
    val parameterMap = parameters(
      "hwDeviceId", hardwareId,
      "now", now
    )

    val pubKeyResults = try {

      val session = neo4jDriver.session
      try {

        // TODO refactor: readTransactionAsync()
        session.readTransaction(new TransactionWork[Set[PublicKey]]() {
          def execute(tx: Transaction): Set[PublicKey] = {

            val result = tx.run(query, parameterMap)
            val records = result.list().toSeq
            logger.debug(s"currentlyValid() -- found ${records.size} results for: hardwareId=$hardwareId; now=$now")

            recordsToPublicKeys(records, "pubKey")

          }
        })

      } finally if (session != null) session.close()

    } catch {

      case su: ServiceUnavailableException =>

        logger.error(s"currentlyValid() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
        Set.empty[PublicKey]

      case e: Exception =>

        logger.error(s"currentlyValid() -- Exception: e.message=${e.getMessage}", e)
        Set.empty[PublicKey]

      case re: RuntimeException =>

        logger.error(s"currentlyValid() -- RuntimeException: re.message=${re.getMessage}", re)
        Set.empty[PublicKey]

    }

    Future(pubKeyResults)

  }

  def findByPubKey(pubKey: String)
                  (implicit neo4jDriver: Driver): Future[Option[PublicKey]] = {

    logger.debug(s"findByPubKey($pubKey)")

    val query =
      """MATCH (pubKey: PublicKey)
        |WHERE pubKey.infoPubKey = $infoPubKey
        |RETURN pubKey""".stripMargin
    val parameterMap = parameters("infoPubKey", pubKey)

    val pubKeyResult = try {

      val session = neo4jDriver.session
      try {

        // TODO refactor: readTransactionAsync()
        session.readTransaction(new TransactionWork[Option[PublicKey]]() {
          def execute(tx: Transaction): Option[PublicKey] = {

            val result = tx.run(query, parameterMap)
            val records = result.list().toSeq
            logger.debug(s"found ${records.size} results for pubKey=$pubKey")

            recordsToPublicKeys(records, "pubKey").headOption

          }
        })

      } finally if (session != null) session.close()

    } catch {

      case su: ServiceUnavailableException =>

        logger.error(s"findByPubKey() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
        None

      case e: Exception =>

        logger.error(s"findByPubKey() -- Exception: e.message=${e.getMessage}", e)
        None

      case re: RuntimeException =>

        logger.error(s"findByPubKey() -- RuntimeException: re.message=${re.getMessage}", re)
        None

    }

    Future(pubKeyResult)

  }

  /**
    * @param pubKeyDelete public key to delete
    * @param neo4jDriver  database connection
    * @return true if deleted (idempotent); false in case of an error (exception or invalid signature)
    */
  def deleteByPubKey(pubKeyDelete: PublicKeyDelete)
                    (implicit neo4jDriver: Driver): Future[Boolean] = {

    val validSignature = EccUtil.validateSignature(pubKeyDelete.publicKey, pubKeyDelete.signature, pubKeyDelete.publicKey)
    if (validSignature) {

      val query =
        """MATCH (pubKey: PublicKey)
          |WHERE pubKey.infoPubKey = $infoPubKey
          |DELETE pubKey""".stripMargin
      val parameterMap = parameters("infoPubKey", pubKeyDelete.publicKey)

      val deleteResult: Boolean = try {

        val session = neo4jDriver.session
        try {

          // TODO refactor: writeTransactionAsync()
          session.writeTransaction(new TransactionWork[Boolean]() {
            def execute(tx: Transaction): Boolean = {

              val result = tx.run(query, parameterMap)
              logger.debug(s"deleted publicKey=${pubKeyDelete.publicKey}")

              true

            }
          })

        } finally if (session != null) session.close()

      } catch {

        case su: ServiceUnavailableException =>

          logger.error(s"deleteByPubKey() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
          false

        case e: Exception =>

          logger.error(s"deleteByPubKey() -- Exception: e.message=${e.getMessage}", e)
          false

        case re: RuntimeException =>

          logger.error(s"deleteByPubKey() -- RuntimeException: re.message=${re.getMessage}", re)
          false

      }

      Future(deleteResult)


    } else {
      logger.error(s"unable to delete public key with invalid signature: $pubKeyDelete")
      Future(false)
    }

  }

  // TODO implement findByHwDeviceId()

  private def toKeyValueMap(publicKey: PublicKey): Map[String, Any] = {

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

  private def recordsToPublicKeys(records: Seq[Record], recordLabel: String): Set[PublicKey] = {

    records map { record =>

      printRecord(record)
      val pubKey = record.get(recordLabel)

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

    } toSet

  }

  private def printRecord(record: Record): Unit = {

    record.fields() foreach { keyValue =>
      println(s"${keyValue.key()}=${keyValue.value()}")
    }

  }

}
