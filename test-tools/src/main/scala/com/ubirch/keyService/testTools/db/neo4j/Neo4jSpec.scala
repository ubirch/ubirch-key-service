package com.ubirch.keyService.testTools.db.neo4j

import com.ubirch.keyservice.config.{Config, Neo4jConfig}

import org.anormcypher.{Cypher, Neo4jConnection, Neo4jREST}
import org.scalatest.{AsyncFeatureSpec, BeforeAndAfterAll, BeforeAndAfterEach, Matchers}

import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSClient

import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-05-09
  */
trait Neo4jSpec extends AsyncFeatureSpec
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  protected implicit val wsClient: WSClient = NingWSClient()
  protected val neo4jConfig: Neo4jConfig = Config.neo4jConfig()
  protected implicit val neo4jConnection: Neo4jConnection = Neo4jREST(
    host = neo4jConfig.host,
    port = neo4jConfig.port,
    username = neo4jConfig.userName,
    password = neo4jConfig.password,
    https = neo4jConfig.https
  )

  override protected def beforeEach(): Unit = {

    /*
    val deletedRelationships = Cypher("MATCH (n)-[r]-(m) DELETE n, r, m").execute()
    if (!deletedRelationships) {
      fail("failed to delete nodes in a relationship")
    }
    */

    val deletedFreeNodes = Cypher("MATCH (n) DELETE n").execute()
    if (!deletedFreeNodes) {
      fail(s"failed to delete free nodes ($deletedFreeNodes)")
    }

  }

  override protected def afterAll(): Unit = wsClient.close()

}
