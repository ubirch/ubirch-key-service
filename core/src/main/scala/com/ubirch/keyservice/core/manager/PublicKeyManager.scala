package com.ubirch.keyservice.core.manager

import java.util.Base64

import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.key.model.db.{PublicKey, PublicKeyDelete, SignedRevoke}
import com.ubirch.keyservice.core.manager.util.DbModelUtils
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil
import com.ubirch.util.json.Json4sUtil
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

  def update(pubKey: PublicKey)
            (implicit neo4jDriver: Driver): Future[Either[UpdateException, PublicKey]] = {

    findByPubKey(pubKey.pubKeyInfo.pubKey) map {

      case None =>

        Left(new UpdateException("failed to update public key as it does not exist"))

      case Some(existingPublicKey: PublicKey) =>

        if (existingPublicKey.signedRevoke.isDefined) {

          Left(new UpdateException("unable to remove revokation from public key"))

        } else {

          val data = DbModelUtils.publicKeyToStringSET(pubKey, "pubKey.")
          val query =
            s"""MATCH (pubKey:PublicKey {infoPubKey: '${pubKey.pubKeyInfo.pubKey}'})
               | SET $data
               | RETURN pubKey""".stripMargin
          logger.debug(s"update() -- query: $query")

          try {

            val session = neo4jDriver.session
            try {

              session.writeTransaction(new TransactionWork[Either[UpdateException, PublicKey]]() {
                def execute(tx: Transaction): Either[UpdateException, PublicKey] = {

                  val result = tx.run(query)
                  val records = result.list().toSeq
                  logger.info(s"update() -- found ${records.size} results for pubKey=$pubKey")

                  val convertedResults = DbModelUtils.recordsToPublicKeys(records, "pubKey")
                  if (convertedResults.size == 1) {
                    Right(DbModelUtils.recordsToPublicKeys(records, "pubKey").head)
                  } else {
                    logger.error(s"update() -- failed to update public key (result.size=${convertedResults.size}): $pubKey")
                    Left(new UpdateException(s"failed to update public key: $pubKey"))
                  }

                }
              })

            } finally if (session != null) session.close()

          } catch {

            case su: ServiceUnavailableException =>

              logger.error(s"update() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
              Left(new UpdateException(s"failed to update public key: ${pubKey.pubKeyInfo.pubKey}"))

            case e: Exception =>

              logger.error(s"update() -- Exception: e.message=${e.getMessage}", e)
              Left(new UpdateException(s"failed to update public key: ${pubKey.pubKeyInfo.pubKey}"))

            case re: RuntimeException =>

              logger.error(s"update() -- RuntimeException: re.message=${re.getMessage}", re)
              Left(new UpdateException(s"failed to update public key: ${pubKey.pubKeyInfo.pubKey}"))

          }

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
        |  AND NOT EXISTS(pubKey.revokeSignature)
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
    val pubKeyB64 = Base64.getDecoder.decode(pubKeyDelete.publicKey)
    val pubKey = GeneratorKeyFactory.getPubKey(pubKeyB64, PublicKeyUtil.associateCurve(pubKeyDelete.curveAlgorithm))
    val validSignature = pubKey.verify(Base64.getDecoder.decode(pubKeyDelete.publicKey) ,
      Base64.getDecoder.decode(pubKeyDelete.signature))
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

  def revoke(signedRevoke: SignedRevoke)
            (implicit neo4jDriver: Driver): Future[Either[KeyRevokeException, PublicKey]] = {

    val payloadJson = Json4sUtil.any2String(signedRevoke.revokation).get
    val pubKey = GeneratorKeyFactory.getPubKey(Base64.getDecoder.decode(signedRevoke.revokation.publicKey), Curve.Ed25519)
    val signatureValid = pubKey.verify(payloadJson.getBytes, Base64.getDecoder.decode(signedRevoke.signature))

    if (signatureValid) {

      findByPubKey(signedRevoke.revokation.publicKey) flatMap {

        case None =>

          Future(Left(new KeyRevokeException("unable to revoke public key if it does not exist")))

        case Some(pubKeyDb) =>

          if (pubKeyDb.signedRevoke.isDefined) {
            Future(Left(new KeyRevokeException("unable to revoke public key if it has been revoked already")))
          } else {

            val revokedKey = pubKeyDb.copy(signedRevoke = Some(signedRevoke))
            update(revokedKey) map {

              case Left(t) =>

                Left(new KeyRevokeException("failed to revoke public key", t))

              case Right(revokedPubKey) =>

                Right(revokedKey)

            }

          }

      }

    } else {

      logger.error(s"revoke() -- failed to verify signature: signedRevoke=$signedRevoke")
      Future(Left(new KeyRevokeException("signature verification failed")))

    }

  }

}

class UpdateException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)

class KeyRevokeException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)
