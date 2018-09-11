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
    // TODO continue tests of relationship being upserted (e.g. trustLevel changes)
    val payloadJson = Json4sUtil.any2String(signedTrust.trustRelation).get
    val signatureValid = EccUtil.validateSignature(
      publicKey = signedTrust.trustRelation.sourcePublicKey,
      signature = signedTrust.signature,
      payload = payloadJson
    )

    if (signatureValid) {

      val query =
        s"""MATCH (source: PublicKey), (target: PublicKey)
          | WHERE source.infoPubKey = '${signedTrust.trustRelation.sourcePublicKey}' AND target.infoPubKey = '${signedTrust.trustRelation.targetPublicKey}'
          | MERGE (source)-[trust:TRUST ${entityToString(signedTrust)}]->(target)
          | RETURN trust""".stripMargin
      val createResult = try {

        val session = neo4jDriver.session
        try {

          session.writeTransaction(new TransactionWork[Either[ExpressingTrustException, SignedTrustRelation]]() {
            def execute(tx: Transaction): Either[ExpressingTrustException, SignedTrustRelation] = {

              val result = tx.run(query)
              val records = result.list().toSeq
              logger.info(s"found ${records.size} results for trust relationship=$signedTrust")
              val convertedResults = recordsToSignedTrustRelationship(records, "trust")

              if (convertedResults.isEmpty) {
                logger.error(s"create() -- failed writing trust relationship with probably at least one key missing in database: signedTTrust=$signedTrust")
                Left(new ExpressingTrustException(s"it seems not all public key in the trust relationship are in our database. are you sure all of them have been uploaded?"))
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
      Future(createResult)

    } else {

      logger.error(s"create() -- failed to verify signature: signedTrust=$signedTrust")
      new ExpressingTrustException()
      Future(Left(new ExpressingTrustException("signature verification failed")))

    }

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
