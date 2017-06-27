package com.ubirch.keyservice.server.actor

import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.core.manager.DeepCheckManager
import com.ubirch.util.deepCheck.model.{DeepCheckRequest, DeepCheckResponse}

import org.anormcypher.Neo4jConnection

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool

/**
  * author: cvandrei
  * since: 2017-06-08
  */
class DeepCheckActor(implicit neo4jConnection: Neo4jConnection)
  extends Actor
    with ActorLogging {

  override def receive: Receive = {

    case _: DeepCheckRequest => context.sender() ! deepCheck()

    case _ => log.error("unknown message")

  }

  private def deepCheck(): DeepCheckResponse = DeepCheckManager.connectivityCheck()

}

object DeepCheckActor {

  def props()(implicit neo4jConnection: Neo4jConnection): Props = {
    new RoundRobinPool(Config.akkaNumberOfWorkers).props(Props(new DeepCheckActor))
  }

}
