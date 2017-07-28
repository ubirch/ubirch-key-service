package com.ubirch.keyservice.core.manager

import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.deepCheck.util.DeepCheckResponseUtil

import org.anormcypher.{Cypher, Neo4jConnection}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * author: cvandrei
  * since: 2017-06-08
  */
object DeepCheckManager {

  def connectivityCheck()(implicit neo4jConnection: Neo4jConnection): DeepCheckResponse = {

    val res = try {

      Cypher("MATCH (n) RETURN n LIMIT 1")()
      DeepCheckResponse()

    } catch {

      case t: Throwable =>
        DeepCheckResponse(
          status = false,
          messages = Seq(t.getMessage)
        )

    }

    DeepCheckResponseUtil.addServicePrefix("key-service", res)

  }

}
