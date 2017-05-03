package com.ubirch.keyservice.core.manager

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.db.Neo4jLabels
import com.ubirch.key.model.rest.PublicKey

import org.anormcypher.{Cypher, Neo4jConnection}

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
    // TODO optional fields: infoValidNotAfter, previousPubKeySignature
    val data =
    s"""{
      |  infoHwDeviceId: "${toCreate.pubkeyInfo.hwDeviceId}",
      |  infoPubKey: "${toCreate.pubkeyInfo.pubKey}",
      |  infoAlgorithm: "${toCreate.pubkeyInfo.algorithm}",
      |  infoPreviousPubKey: "${toCreate.pubkeyInfo.previousPubKey}",
      |  infoCreated: "${toCreate.pubkeyInfo.created}",
      |  infoValidNotBefore: "${toCreate.pubkeyInfo.validNotBefore}",
      |  infoValidNotAfter: "${toCreate.pubkeyInfo.validNotAfter}",
      |  signature: "${toCreate.signature}",
      |  previousPubKeySignature: "${toCreate.previousPubKeySignature}"
      |}""".stripMargin

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
         |WHERE pubKey.hwDeviceId = {hwDeviceId}
         |RETURN pubKey
       """.stripMargin
    ).on("hwDeviceId" -> hardwareId)()
    logger.debug(s"found ${result.size} results for hardwareId=$hardwareId")

    // TODO map result to Set[PublicKey]

    // TODO Future doesn't seem to be necessary...remove?
    Future(Set())

  }

}
