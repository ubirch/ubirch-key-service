package com.ubirch.keyservice.server.actor

import com.ubirch.key.model._
import com.ubirch.key.model.rest.{PublicKey, PublicKeyDelete, PublicKeys, SignedRevoke}
import com.ubirch.keyservice.config.KeySvcConfig
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonErrorResponse
import org.neo4j.driver.v1.Driver
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyActor(implicit neo4jDriver: Driver) extends Actor
  with ActorLogging {

  override def receive: Receive = {

    case create: CreatePublicKey =>

      val sender = context.sender()
      val dbPublicKey = Json4sUtil.any2any[db.PublicKey](create.publicKey)
      try {
        PublicKeyManager.create(dbPublicKey) onComplete {
          case Success(Right(dbPubKey)) =>
            sender ! (dbPubKey map Json4sUtil.any2any[rest.PublicKey])
          case Success(Left(t)) =>
            sender ! JsonErrorResponse(errorType = "Invalid Input", errorMessage = t.getMessage)
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
        PublicKeyManager.currentlyValid(queryCurrentlyValid.hardwareId).onComplete {
          case Success(dbPubKeys) =>
            sender ! PublicKeys(dbPubKeys map Json4sUtil.any2any[rest.PublicKey])
          case Failure(t) =>
            throw t
        }
      }
      catch {
        case e: Exception =>
          log.error(e, "queryCurrentlyValid")
          sender ! JsonErrorResponse(errorType = "KeyError", errorMessage = e.getMessage)
      }

    case byPublicKey: ByPublicKey =>

      val sender = context.sender()
      PublicKeyManager.findByPubKey(byPublicKey.publicKey) map (sender ! _)

    case pubKeyDelete: PublicKeyDelete =>

      val sender = context.sender()
      val dbPubKeyDelete = Json4sUtil.any2any[db.PublicKeyDelete](pubKeyDelete)
      PublicKeyManager.deleteByPubKey(dbPubKeyDelete) map (sender ! _) recoverWith{
        case e: Exception =>
          log.error(e, "something bad happend")
          sender ! JsonErrorResponse(errorType = "KeyError", errorMessage = e.getMessage)
          Future(None)
      }

    case signedRevoke: SignedRevoke =>

      val sender = context.sender()
      executeRevoke(signedRevoke, sender)

  }

  override def unhandled(message: Any): Unit = {

    log.error(s"received unknown message: ${message.toString} (${message.getClass.toGenericString}) from: ${context.sender()}")
    context.sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = s"sorry, we just had a problem")

  }

  private def executeRevoke(signedRevoke: rest.SignedRevoke, sender: ActorRef): Unit = {

    try {

      val signedRevokeDb = Json4sUtil.any2any[db.SignedRevoke](signedRevoke)
      PublicKeyManager.revoke(signedRevokeDb) onComplete {

        case Success(Right(revokedKey)) =>

          sender ! Json4sUtil.any2any[rest.PublicKey](revokedKey)

        case Success(Left(t)) =>

          sender ! JsonErrorResponse(errorType = "RevokeError", errorMessage = t.getMessage)

        case Failure(t) =>

          sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = t.getMessage)

      }

    } catch {

      case e: Exception =>

        sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = e.getMessage)

    }

  }

}

object PublicKeyActor {

  def props()(implicit neo4jDriver: Driver): Props = {
    new RoundRobinPool(KeySvcConfig.akkaNumberOfWorkers).props(Props(new PublicKeyActor))
  }

}

case class CreatePublicKey(publicKey: PublicKey)

case class QueryCurrentlyValid(hardwareId: String)

case class ByPublicKey(publicKey: String)
