package com.ubirch.keyservice.server.actor

import com.ubirch.key.model.rest.{PublicKey, PublicKeys}
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.util.model.JsonErrorResponse

import org.anormcypher.Neo4jConnection

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyActor(implicit neo4jConnection: Neo4jConnection) extends Actor
  with ActorLogging {

  override def receive: Receive = {

    case create: CreatePublicKey =>
      val sender = context.sender()
      PublicKeyManager.create(create.publicKey) map (sender ! _)

    case queryCurrentlyValid: QueryCurrentlyValid =>
      val sender = context.sender()
      PublicKeyManager.currentlyValid(queryCurrentlyValid.hardwareId) map (sender ! PublicKeys(_))

    case _ =>
      log.error("unknown message (PublicKeyActor)")
      sender ! JsonErrorResponse(errorType = "UnknownMessage", errorMessage = "unable to handle message")

  }

}

case class CreatePublicKey(publicKey: PublicKey)

case class QueryCurrentlyValid(hardwareId: String)
