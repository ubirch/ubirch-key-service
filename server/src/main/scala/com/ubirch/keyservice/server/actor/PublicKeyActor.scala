package com.ubirch.keyservice.server.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import com.ubirch.key.model._
import com.ubirch.key.model.rest.{PublicKey, PublicKeys}
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.keyservice.server.actor.util.ModelUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonErrorResponse
import org.anormcypher.Neo4jREST

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

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
      try {
        PublicKeyManager.create(dbPublicKey) onComplete {
          case Success(dbPubKey) =>
            sender ! (dbPubKey map Json4sUtil.any2any[rest.PublicKey])
          case Failure(t) =>
            sender ! JsonErrorResponse(errorType = "Invalid Input", errorMessage = t.getMessage)
        }
      }
      catch {
        case e: Exception =>
          sender ! JsonErrorResponse(errorType = "Invalid Input", errorMessage = e.getMessage)
      }

    case queryCurrentlyValid: QueryCurrentlyValid =>
      val sender = context.sender()
      try {
        PublicKeyManager.currentlyValid(queryCurrentlyValid.hardwareId) map { dbPubKeys =>
          sender ! PublicKeys(dbPubKeys map Json4sUtil.any2any[rest.PublicKey])
        }
      }
      catch {
        case e: Exception =>
          log.error("queryCurrentlyValid", e)
          sender ! JsonErrorResponse(errorType = "KeyError", errorMessage = e.getMessage)
      }

    case byPublicKey: ByPublicKey =>
      val sender = context.sender()
      PublicKeyManager.findByPubKey(byPublicKey.publicKey) map (sender ! _)

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

case class ByPublicKey(publicKey: String)
