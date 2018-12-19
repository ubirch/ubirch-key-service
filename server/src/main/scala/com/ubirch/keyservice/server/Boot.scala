package com.ubirch.keyservice.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.keyservice.config.{KeySvcConfig, KeySvcConfigKeys}
import com.ubirch.keyservice.server.route.MainRoute
import com.ubirch.keyservice.utils.neo4j.Neo4jSchema
import com.ubirch.util.neo4j.utils.{Neo4jDriverUtil, Neo4jUtils}
import org.neo4j.driver.v1.Driver

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-03-22
  */
object Boot extends App with StrictLogging {

  logger.info("key-service will get started ...")

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val timeout: Timeout = Timeout(KeySvcConfig.timeout seconds)

  implicit val neo4jDriver: Driver = {
    try {

      implicit val neo4jDriver: Driver = Neo4jDriverUtil.driver(KeySvcConfigKeys.neo4jConfigPrefix)
      Neo4jUtils.createConstraints(Neo4jSchema.constraints)
      Neo4jUtils.createIndices(Neo4jSchema.indices)
      neo4jDriver
    }
    catch {
      case e: Exception =>
        logger.error("error while setting up neo4j connection", e)
        throw e
    }
  }

  val bindingFuture = start()
  registerShutdownHooks()

  private def start(): Future[ServerBinding] = {

    val interface = KeySvcConfig.interface
    val port = KeySvcConfig.port
    implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

    logger.info(s"start http server on $interface:$port")
    Http().bindAndHandle((new MainRoute).myRoute, interface, port)

  }

  private def registerShutdownHooks(): Unit = {

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {

        neo4jDriver.close()

        bindingFuture
          .flatMap(_.unbind())
          .onComplete(_ => system.terminate())

      }
    })

  }

}
