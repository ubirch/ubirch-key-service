package com.ubirch.keyservice.server.actor

import com.ubirch.key.model._
import com.ubirch.key.model.rest.{PublicKey, PublicKeys}
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.keyservice.server.actor.util.ModelUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonErrorResponse

import org.anormcypher.Neo4jREST

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyActor(implicit neo4jREST: Neo4jREST) extends Actor
  with ActorLogging {

  override def receive: Receive = {

    case create: CreatePublicKey =>
      val sender = context.sender()
      val pubKeyWithId = ModelUtil.withPubKeyId(create.publicKey)
      val dbPublicKey = Json4sUtil.any2any[db.PublicKey](pubKeyWithId)
      PublicKeyManager.create(dbPublicKey) map { dbPubKey =>
        sender ! (dbPubKey map Json4sUtil.any2any[rest.PublicKey])
      }

    case queryCurrentlyValid: QueryCurrentlyValid =>
      val sender = context.sender()
      PublicKeyManager.currentlyValid(queryCurrentlyValid.hardwareId) map { dbPubKeys =>
        sender ! PublicKeys(dbPubKeys map Json4sUtil.any2any[rest.PublicKey])
      }

    case _ =>
      log.error("unknown message (PublicKeyActor)")
      sender ! JsonErrorResponse(errorType = "UnknownMessage", errorMessage = "unable to handle message")

  }

}

object PublicKeyActor {

  def props()(implicit neo4jREST: Neo4jREST): Props = {
    new RoundRobinPool(Config.akkaNumberOfWorkers).props(Props(new PublicKeyActor))
  }

}

case class CreatePublicKey(publicKey: PublicKey)

case class QueryCurrentlyValid(hardwareId: String)
