package com.ubirch.keyservice.server.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import com.ubirch.keyservice.config.KeySvcConfig
import com.ubirch.keyservice.core.manager.DeepCheckManager
import com.ubirch.util.deepCheck.model.DeepCheckRequest
import com.ubirch.util.model.JsonErrorResponse
import org.neo4j.driver.v1.Driver

/**
  * author: cvandrei
  * since: 2017-06-08
  */
class DeepCheckActor(implicit neo4jDriver: Driver)
  extends Actor
    with ActorLogging {

  override def receive: Receive = {

    case _: DeepCheckRequest =>
      val sender = context.sender()
      sender ! DeepCheckManager.connectivityCheck()
  }

  override def unhandled(message: Any): Unit = {
    log.error(s"received unknown message: ${message.toString} (${message.getClass.toGenericString}) from: ${context.sender()}")
    context.sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = s"sorry, we just had a problem")
  }


}

object DeepCheckActor {

  def props()(implicit neo4jDriver: Driver): Props = {
    new RoundRobinPool(KeySvcConfig.akkaNumberOfWorkers).props(Props(new DeepCheckActor))
  }

}
