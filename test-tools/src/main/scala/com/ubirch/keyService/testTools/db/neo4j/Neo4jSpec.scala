package com.ubirch.keyService.testTools.db.neo4j

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.keyservice.config.ConfigKeys
import com.ubirch.keyservice.utils.neo4j.Neo4jSchema
import com.ubirch.util.neo4j.utils.{Neo4jDriverUtil, Neo4jUtils}

import org.neo4j.driver.v1.Driver
import org.scalatest.{AsyncFeatureSpec, BeforeAndAfterAll, BeforeAndAfterEach, Matchers}

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer

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

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val httpClient: HttpExt = Http()

  protected implicit val neo4jDriver: Driver = Neo4jDriverUtil.driver(ConfigKeys.neo4jConfigPrefix)

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

    if (!Neo4jUtils.createConstraints(Neo4jSchema.constraints)) {
      fail("failed to create constraints")
    }

    if (!Neo4jUtils.createIndices(Neo4jSchema.indices)) {
      fail("failed to create indices")
    }

  }

  override protected def afterAll(): Unit = {
    neo4jDriver.close()
    System.exit(0)
  }

}
