package com.ubirch.keyservice.server.actor

import com.ubirch.key.model.rest.PublicKey
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.util.model.JsonErrorResponse

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyActor extends Actor
  with ActorLogging {

  override def receive: Receive = {

    case create: CreatePublicKey =>
      val sender = context.sender()
      PublicKeyManager.create(create.publicKey) map (sender ! _)

    case _ =>
      log.error("unknown message (PublicKeyActor)")
      sender ! JsonErrorResponse(errorType = "UnknownMessage", errorMessage = "unable to handle message")

  }

}

case class CreatePublicKey(publicKey: PublicKey)
