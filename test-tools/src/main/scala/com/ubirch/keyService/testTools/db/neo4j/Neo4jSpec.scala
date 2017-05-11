package com.ubirch.keyService.testTools.db.neo4j

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.keyservice.config.{Config, Neo4jConfig}
import com.ubirch.keyservice.util.neo4j.Neo4jUtils

import org.anormcypher.{Neo4jConnection, Neo4jREST}
import org.scalatest.{AsyncFeatureSpec, BeforeAndAfterAll, BeforeAndAfterEach, Matchers}

import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-05-09
  */
trait Neo4jSpec extends AsyncFeatureSpec
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with StrictLogging {

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

  override protected def beforeEach(): Unit = {

    if (!Neo4jUtils.dropAllConstraints()) {
      fail("failed to drop all constraints")
    }

    if (!Neo4jUtils.dropAllIndices()) {
      fail("failed to drop all indices")
    }

    if (!Neo4jUtils.deleteAllNodesAndRelationships()) {
      fail("failed to delete nodes and possible relationships")
    }

    if (!Neo4jUtils.createConstraints()) {
      fail("failed to create constraints")
    }

    if (!Neo4jUtils.createIndices()) {
      fail("failed to create indices")
    }

  }

  override protected def afterAll(): Unit = wsClient.close()

}
