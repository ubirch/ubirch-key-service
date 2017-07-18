package com.ubirch.keyservice.cmd

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.keyservice.config.{Config, Neo4jConfig}

import org.anormcypher.{Neo4jConnection, Neo4jREST}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContextExecutor

/**
  * author: cvandrei
  * since: 2017-05-10
  */
trait CmdBase extends App
  with StrictLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  protected implicit val wsClient: WSClient = NingWSClient()
  protected val neo4jConfig: Neo4jConfig = Config.neo4jConfig()
  protected implicit val neo4jConnection: Neo4jConnection = Neo4jREST(
    host = neo4jConfig.host,
    port = neo4jConfig.port,
    username = neo4jConfig.userName,
    password = neo4jConfig.password,
    https = neo4jConfig.https
  )
  protected implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  run()

  def close(): Unit = {
    wsClient.close()
    system.terminate()
  }

  def run(): Unit

}
