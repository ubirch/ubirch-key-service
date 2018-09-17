package com.ubirch.keyservice.server.actor

import com.ubirch.key.model._
import com.ubirch.key.model.rest.{FindTrustedSigned, SignedTrustRelation}
import com.ubirch.keyservice.config.KeyConfig
import com.ubirch.keyservice.core.manager.TrustManager
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonErrorResponse

import org.neo4j.driver.v1.Driver

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2018-09-11
  */
class TrustActor(implicit neo4jDriver: Driver) extends Actor
  with ActorLogging {

  implicit val ec: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {

    case signedTrust: SignedTrustRelation =>

      val sender = context.sender()
      executeExpressTrust(signedTrust, sender)

    case signedGetTrusted: FindTrustedSigned =>

      val sender = context.sender()
      executeFindTrusted(signedGetTrusted, sender)

  }

  private def executeExpressTrust(signedTrust: SignedTrustRelation, sender: ActorRef): Unit = {

    val dbSignedTrust = Json4sUtil.any2any[db.SignedTrustRelation](signedTrust)
    try {

      TrustManager.upsert(dbSignedTrust) onComplete {

        case Success(Right(dbResult)) =>

          log.debug(s"executeExpressTrust() -- successful: dbResult=$dbResult")
          val restResult = Json4sUtil.any2any[rest.SignedTrustRelation](dbResult)
          log.debug(s"executeExpressTrust() -- successful: restResult=$restResult")
          sender ! restResult

        case Success(Left(t)) =>

          sender ! JsonErrorResponse(errorType = "TrustError", errorMessage = t.getMessage)

        case Failure(t) =>

          sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = t.getMessage)

      }

    } catch {

      case e: Exception =>

        sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = e.getMessage)

    }

  }

  private def executeFindTrusted(findTrustedSigned: rest.FindTrustedSigned, sender: ActorRef): Unit = {

    try {

      val findTrustedSignedDb = Json4sUtil.any2any[db.FindTrustedSigned](findTrustedSigned)
      TrustManager.findTrusted(findTrustedSignedDb) onComplete {

        case Success(Right(trustedKeys)) =>

          val trustedKeysResult = TrustedKeyResultSet(
            trustedKeys map Json4sUtil.any2any[rest.TrustedKeyResult]
          )
          sender ! trustedKeysResult

        case Success(Left(t)) =>

          sender ! JsonErrorResponse(errorType = "FindTrustedError", errorMessage = t.getMessage)

        case Failure(t) =>

          sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = t.getMessage)

      }

    } catch {

      case e: Exception =>

        sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = e.getMessage)

    }

  }

  override def unhandled(message: Any): Unit = {

    log.error(s"received unknown message: ${message.toString} (${message.getClass.toGenericString}) from: ${context.sender()}")
    context.sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = s"sorry, we just had a problem")

  }

}

object TrustActor {

  def props()(implicit neo4jDriver: Driver): Props = {
    new RoundRobinPool(KeyConfig.akkaNumberOfWorkers).props(Props(new TrustActor))
  }

}

case class TrustedKeyResultSet(trusted: Set[rest.TrustedKeyResult])
