package com.ubirch.keyservice.core.manager

import java.util.Base64

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyDelete, SignedRevoke}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil

import org.joda.time.{DateTime, DateTimeZone}
import org.neo4j.driver.v1.Values.parameters
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException
import org.neo4j.driver.v1.{Driver, Transaction, TransactionWork}

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
    * @param pubKey      public key to persist
    * @param neo4jDriver Neo4j connection
    * @return persisted public key; None if something went wrong
    */
  def create(pubKey: PublicKey)
            (implicit neo4jDriver: Driver): Future[Either[Exception, Option[PublicKey]]] = {

    findByPubKey(pubKey.pubKeyInfo.pubKey) flatMap {

      case Some(exPubKey: PublicKey) =>

        if (pubKey.pubKeyInfo.pubKey == exPubKey.pubKeyInfo.pubKey &&
          pubKey.pubKeyInfo.hwDeviceId == exPubKey.pubKeyInfo.hwDeviceId) {
          Future(Right(Some(exPubKey)))
        }
        else {
          val errMsg = s"unable to create publicKey if it already exists: ${pubKey.pubKeyInfo.pubKey}"
          logger.error(errMsg)
          Future(Left(new Exception(errMsg)))
        }
      case None =>

        if (PublicKeyUtil.validateSignature(pubKey)) {

          val data = DbModelUtils.publicKeyToString(pubKey)
          val query =
            s"""CREATE (pubKey:PublicKey $data)
               |RETURN pubKey""".stripMargin

          val createResult = try {

            val session = neo4jDriver.session
            try {

              session.writeTransaction(new TransactionWork[Either[Exception, Option[PublicKey]]]() {
                def execute(tx: Transaction): Either[Exception, Option[PublicKey]] = {

                  val result = tx.run(query)
                  val records = result.list().toSeq
                  logger.info(s"found ${records.size} results for pubKey=$pubKey")

                  Right(DbModelUtils.recordsToPublicKeys(records, "pubKey").headOption)

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

        session.readTransaction(new TransactionWork[Set[PublicKey]]() {
          def execute(tx: Transaction): Set[PublicKey] = {

            val result = tx.run(query, parameterMap)
            val records = result.list().toSeq
            logger.info(s"currentlyValid() -- found ${records.size} results for: hardwareId=$hardwareId; now=$now")

            DbModelUtils.recordsToPublicKeys(records, "pubKey")

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

        session.readTransaction(new TransactionWork[Option[PublicKey]]() {
          def execute(tx: Transaction): Option[PublicKey] = {

            val result = tx.run(query, parameterMap)
            val records = result.list().toSeq
            logger.info(s"found ${records.size} results for pubKey=$pubKey")

            DbModelUtils.recordsToPublicKeys(records, "pubKey").headOption

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

    val decodedPubKey = Base64.getDecoder.decode(pubKeyDelete.publicKey)
    val validSignature = EccUtil.validateSignature(pubKeyDelete.publicKey, pubKeyDelete.signature, decodedPubKey)
    if (validSignature) {

      val query =
        """MATCH (pubKey: PublicKey)
          |WHERE pubKey.infoPubKey = $infoPubKey
          |DELETE pubKey""".stripMargin
      val parameterMap = parameters("infoPubKey", pubKeyDelete.publicKey)

      val deleteResult: Boolean = try {

        val session = neo4jDriver.session
        try {

          session.writeTransaction(new TransactionWork[Boolean]() {
            def execute(tx: Transaction): Boolean = {

              tx.run(query, parameterMap)
              logger.info(s"deleted publicKey=${pubKeyDelete.publicKey}")

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

  def revoke(signedRevoke: SignedRevoke): Future[Either[KeyRevokeException, Boolean]] = {

    // TODO (UP-178) automated tests
    // TODO (UP-177) implement
    Future(Right(true))

  }

}

class KeyRevokeException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)
