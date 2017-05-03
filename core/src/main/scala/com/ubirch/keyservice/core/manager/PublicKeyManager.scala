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
    // TODO use actual fields
    val json = """{name:"AnormCypher"}""".stripMargin

    val result = Cypher(
      s"""CREATE (pubKey:${Neo4jLabels.PUBLIC_KEY} $json)
         |RETURN pubKey""".stripMargin
    ).execute()

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
    // TODO query Neo4j
    Future(Set())

  }

}
