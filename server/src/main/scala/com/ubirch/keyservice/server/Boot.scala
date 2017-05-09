package com.ubirch.keyservice.server

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.server.route.MainRoute

import org.anormcypher.{Neo4jConnection, Neo4jREST}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.util.Timeout
import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-03-22
  */
object Boot extends App with StrictLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val timeout = Timeout(Config.timeout seconds)

  implicit val wsClient: WSClient = NingWSClient()
  val neo4jConfig = Config.neo4jConfig()
  implicit val neo4jConnection: Neo4jConnection = Neo4jREST(
    host = neo4jConfig.host,
    port = neo4jConfig.port,
    username = neo4jConfig.userName,
    password = neo4jConfig.password,
    https = neo4jConfig.https
  )

  val bindingFuture = start()
  registerShutdownHooks()

  private def start(): Future[ServerBinding] = {

    val interface = Config.interface
    val port = Config.port
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)

    logger.info(s"start http server on $interface:$port")
    Http().bindAndHandle((new MainRoute).myRoute, interface, port)

  }

  private def registerShutdownHooks() = {

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {

        bindingFuture
          .flatMap(_.unbind())
          .onComplete(_ => system.terminate())

        wsClient.close()

      }
    })

  }

}
