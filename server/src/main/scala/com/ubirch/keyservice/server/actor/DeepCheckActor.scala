package com.ubirch.keyservice.server.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Status}
import akka.pattern._
import akka.routing.RoundRobinPool
import com.ubirch.keyservice.config.KeySvcConfig
import com.ubirch.keyservice.core.manager.DeepCheckManager
import com.ubirch.keyservice.server.route.WithCircuitBreaker
import com.ubirch.util.deepCheck.model.{DeepCheckRequest, DeepCheckResponse}
import com.ubirch.util.model.JsonErrorResponse
import org.neo4j.driver.v1.Driver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-06-08
  */
class DeepCheckActor(implicit neo4jDriver: Driver)
  extends Actor
    with WithCircuitBreaker
    with ActorLogging {

  override lazy val defaultCircuitBreaker: CircuitBreaker = getCircuitBreaker(callTimeout = 2 seconds)

  override def system: ActorSystem = context.system

  override def receive: Receive = {

    case _: DeepCheckRequest =>
      val sender = context.sender()

      defaultCircuitBreaker
        .onCallTimeout(_ => sender ! DeepCheckResponse(status = false, messages = Seq("Deepcheck has timed out.")))
        .onCallFailure(_ => sender ! DeepCheckResponse(status = false, messages = Seq("Deepcheck has failed")))
        .withCircuitBreaker(Future(DeepCheckManager.connectivityCheck()))
        .map(x => DeepCheckActor.SendReplyTo(sender, x))
        .pipeTo(self)

    case DeepCheckActor.SendReplyTo(sender, res) =>
      sender ! res

    case Status.Failure(t) =>
      val errors = Seq(t.getMessage)
      log.error(errors.mkString(" "))

  }

  override def unhandled(message: Any): Unit = {
    log.error(s"received unknown message: ${message.toString} (${message.getClass.toGenericString}) from: ${context.sender()}")
    context.sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = s"sorry, we just had a problem")
  }


}

object DeepCheckActor {

  case class SendReplyTo(sender: ActorRef, res: DeepCheckResponse)

  def props()(implicit neo4jDriver: Driver): Props = {
    new RoundRobinPool(KeySvcConfig.akkaNumberOfWorkers).props(Props(new DeepCheckActor))
  }

}
