package com.ubirch.keyservice.core.manager

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{SignedTrustRelation, TrustRelation}
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.neo4j.utils.Neo4jParseUtil

import org.neo4j.driver.v1.exceptions.ServiceUnavailableException
import org.neo4j.driver.v1.{Driver, Record, Transaction, TransactionWork}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps


/**
  * author: cvandrei
  * since: 2018-09-11
  */
object TrustManager extends StrictLogging {

  def upsert(signedTrust: SignedTrustRelation)
            (implicit neo4jDriver: Driver): Future[Either[ExpressingTrustException, SignedTrustRelation]] = {

    // TODO automated tests
    val payloadJson = Json4sUtil.any2String(signedTrust.trustRelation).get
    val signatureValid = EccUtil.validateSignature(
      publicKey = signedTrust.trustRelation.sourcePublicKey,
      signature = signedTrust.signature,
      payload = payloadJson
    )

    if (signatureValid) {

      val trustLevel = signedTrust.trustRelation.trustLevel
      if (trustLevel < 1 || trustLevel > 100) {

        logger.error(s"create() -- invalid trustLevel value: signedTrust=$signedTrust")
        Future(Left(new ExpressingTrustException(s"invalid trustLevel value")))

      } else {

        delete(signedTrust) map {

          case Left(e: DeleteTrustException) =>

            logger.error(s"create() -- DeleteTrustException: e.message=${e.getMessage}", e)
            Left(new ExpressingTrustException(s"failed to create trust relationship: $signedTrust"))

          case Right(false) =>

            logger.error(s"create() -- failed to delete existing trust relationship before creating the latest version")
            Left(new ExpressingTrustException(s"failed to create trust relationship: $signedTrust"))

          case Right(true) =>

            val srcPubKey = signedTrust.trustRelation.sourcePublicKey
            val targetPubKey = signedTrust.trustRelation.targetPublicKey

            val query =
              s"""MATCH (source: PublicKey), (target: PublicKey)
                 | WHERE source.infoPubKey = '$srcPubKey' AND target.infoPubKey = '$targetPubKey'
                 | CREATE (source)-[trust:TRUST ${entityToString(signedTrust)}]->(target)
                 | RETURN trust""".stripMargin
            logger.debug(s"Cypher query: $query")
            val createResult = try {

              val session = neo4jDriver.session
              try {

                session.writeTransaction(new TransactionWork[Either[ExpressingTrustException, SignedTrustRelation]]() {
                  def execute(tx: Transaction): Either[ExpressingTrustException, SignedTrustRelation] = {

                    val result = tx.run(query)
                    val records = result.list().toSeq
                    logger.info(s"create() -- found ${records.size} results for trust relationship=$signedTrust")
                    val convertedResults = recordsToSignedTrustRelationship(records, "trust")

                    if (convertedResults.isEmpty) {
                      logger.error(s"create() -- failed writing trust relationship with probably at least one key missing in database: signedTTrust=$signedTrust")
                      Left(new ExpressingTrustException(s"it seems not all public keys in the trust relationship are in our database. are you sure all of them have been uploaded?"))
                    } else if (convertedResults.size == 1) {
                      Right(convertedResults.head)
                    } else {
                      logger.error(s"create() -- failed while writing trust relationship to database: convertedResults.size=${convertedResults.size}; signedTTrust=$signedTrust")
                      Left(new ExpressingTrustException(s"failed while writing trust relationship to database"))
                    }

                  }
                })

              } finally if (session != null) session.close()

            } catch {

              case su: ServiceUnavailableException =>

                logger.error(s"create() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
                Left(new ExpressingTrustException(s"failed to create trust relationship: $signedTrust"))

              case e: Exception =>

                logger.error(s"create() -- Exception: e.message=${e.getMessage}", e)
                Left(new ExpressingTrustException(s"failed to create trust relationship: $signedTrust"))

              case re: RuntimeException =>

                logger.error(s"create() -- RuntimeException: re.message=${re.getMessage}", re)
                Left(new ExpressingTrustException(s"failed to create trust relationship: $signedTrust"))

            }
            createResult

        }

      }

    } else {

      logger.error(s"create() -- failed to verify signature: signedTrust=$signedTrust")
      new ExpressingTrustException()
      Future(Left(new ExpressingTrustException("signature verification failed")))

    }

  }

  def delete(signedTrust: SignedTrustRelation)
            (implicit neo4jDriver: Driver): Future[Either[DeleteTrustException, Boolean]] = {

    // TODO automated tests
    val srcPubKey = signedTrust.trustRelation.sourcePublicKey
    val targetPubKey = signedTrust.trustRelation.targetPublicKey

    val query = s"""MATCH ()-[trust:TRUST {trustSource: '$srcPubKey', trustTarget: '$targetPubKey'}]->()  DELETE trust RETURN trust""".stripMargin
    logger.debug(s"delete() -- query=$query")

    val deleteResult = try {

      val session = neo4jDriver.session
      try {

        session.writeTransaction(new TransactionWork[Either[DeleteTrustException, Boolean]]() {
          def execute(tx: Transaction): Either[DeleteTrustException, Boolean] = {

            val result = tx.run(query)
            val recordsDeleted = result.list()
            logger.info(s"delete() -- found ${recordsDeleted.size} results for trust relationship=$signedTrust")

            if (recordsDeleted.isEmpty || recordsDeleted.size == 1) {
              logger.debug(s"delete() -- deleted ${recordsDeleted.size} trust relationships: signedTTrust=$signedTrust")
              Right(true)
            } else {
              logger.error(s"delete() -- deleted ${recordsDeleted.size} trust relationships insetad of just one: signedTTrust=$signedTrust")
              Left(new DeleteTrustException(s"deleted too many trust relationships"))
            }

          }
        })

      } finally if (session != null) session.close()

    } catch {

      case su: ServiceUnavailableException =>

        logger.error(s"delete() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
        Left(new DeleteTrustException(s"failed to delete trust relationship: $signedTrust"))

      case e: Exception =>

        logger.error(s"delete() -- Exception: e.message=${e.getMessage}", e)
        Left(new DeleteTrustException(s"failed to delete trust relationship: $signedTrust"))

      case re: RuntimeException =>

        logger.error(s"delete() -- RuntimeException: re.message=${re.getMessage}", re)
        Left(new DeleteTrustException(s"failed to delete trust relationship: $signedTrust"))

    }
    Future(deleteResult)

  }

  def findBySourceTarget(sourcePubKey: String, targetPubKey: String)
            (implicit neo4jDriver: Driver): Future[Either[FindTrustException, Option[SignedTrustRelation]]] = {

    // TODO automated tests
    val query = s"""MATCH ()-[trust:TRUST {trustSource: '$sourcePubKey', trustTarget: '$targetPubKey'}]->() RETURN trust""".stripMargin
    logger.debug(s"findSourceTarget() -- query=$query")

    val findResult = try {

      val session = neo4jDriver.session
      try {

        session.writeTransaction(new TransactionWork[Either[FindTrustException, Option[SignedTrustRelation]]]() {
          def execute(tx: Transaction): Either[FindTrustException, Option[SignedTrustRelation]] = {

            val result = tx.run(query)
            val recordsFound = result.list().toSeq
            logger.info(s"findSourceTarget() -- found ${recordsFound.size} results for: sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey")
            val convertedResults = recordsToSignedTrustRelationship(recordsFound, "trust")

            if (convertedResults.isEmpty) {
              logger.debug(s"findSourceTarget() -- failed finding trust relationship in database: sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey")
              Right(convertedResults.headOption)
            } else {
              logger.error(s"findSourceTarget() -- failed while finding trust relationship in database: convertedResults.size=${convertedResults.size}; sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey")
              Left(new FindTrustException(s"failed while writing trust relationship to database"))
            }

          }
        })

      } finally if (session != null) session.close()

    } catch {

      case su: ServiceUnavailableException =>

        logger.error(s"findSourceTarget() -- ServiceUnavailableException: su.message=${su.getMessage}", su)
        Left(new FindTrustException(s"failed to find trust relationship: sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey"))

      case e: Exception =>

        logger.error(s"findSourceTarget() -- Exception: sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey, e.message=${e.getMessage}", e)
        Left(new FindTrustException(s"failed to find trust relationship: sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey"))

      case re: RuntimeException =>

        logger.error(s"findSourceTarget() -- RuntimeException: sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey, re.message=${re.getMessage}", re)
        Left(new FindTrustException(s"failed to find trust relationship: sourcePubKey=$sourcePubKey, targetPubKey=$targetPubKey"))

    }
    Future(findResult)

  }

  private def toKeyValueMap(signedTrustRelation: SignedTrustRelation): Map[String, Any] = {

    var keyValue: Map[String, Any] = Map(
      "signature" -> signedTrustRelation.signature,
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

  private def entityToString(signedTrustRelation: SignedTrustRelation): String = {
    val keyValue = toKeyValueMap(signedTrustRelation)
    Neo4jParseUtil.keyValueToString(keyValue)
  }

  private def recordsToSignedTrustRelationship(records: Seq[Record], recordLabel: String): Set[SignedTrustRelation] = {

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
        signature = Neo4jParseUtil.asType[String](trustRelation, "signature")
      )

    } toSet

  }

}

class ExpressingTrustException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)

class DeleteTrustException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)

class FindTrustException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)
