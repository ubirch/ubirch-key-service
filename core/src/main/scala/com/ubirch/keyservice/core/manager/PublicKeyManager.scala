package com.ubirch.keyservice.core.manager

import com.ubirch.key.model.rest.PublicKey

import org.anormcypher.Neo4jConnection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-04-27
  */
object PublicKeyManager {

  def create(toCreate: PublicKey)
            (implicit neo4jConnection: Neo4jConnection): Future[Option[PublicKey]] = {

    // TODO automated tests
    // TODO store in Neo4j
    Future(Some(toCreate))

  }

  def currentlyValid(hardwareId: String)
                    (implicit neo4jConnection: Neo4jConnection): Future[Set[PublicKey]] = {

    // TODO automated tests
    // TODO query Neo4j
    Future(Set())

  }

}
