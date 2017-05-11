package com.ubirch.keyservice.util.neo4j

import com.typesafe.scalalogging.slf4j.StrictLogging

import org.anormcypher.{Cypher, Neo4jConnection}

import scala.concurrent.ExecutionContext

/**
  * author: cvandrei
  * since: 2017-05-10
  */
object Neo4jUtils extends StrictLogging {

  def createConstraints()
                       (implicit neo4jConnection: Neo4jConnection, ec: ExecutionContext): Boolean = {

    val results = Neo4jConstraints.constraints map { constraint =>

      if (Cypher(s"CREATE $constraint").execute()) {
        logger.info(s"created constraint (or it already existed): $constraint")
        true
      } else {
        logger.error("failed to create constraint")
        false
      }

    }

    results.forall(b => b)

  }

  def dropAllConstraints()
                        (implicit neo4jConnection: Neo4jConnection, ec: ExecutionContext): Boolean = {

    val results = queryConstraints() map { constraint =>

      if (Cypher(s"DROP $constraint").execute()) {
        logger.info(s"dropped constraint: $constraint")
        true
      } else {
        logger.error(s"failed to drop constraint: $constraint")
        false
      }

    }

    results.forall(b => b)

  }

  def queryConstraints()
                      (implicit neo4jConnection: Neo4jConnection, ec: ExecutionContext): Seq[String] = {

    Cypher("CALL db.constraints()")() map { row =>
      row[String]("description")
    }

  }

  def deleteAllNodesAndRelationships()
                                    (implicit neo4jConnection: Neo4jConnection, ec: ExecutionContext): Boolean = {

    deleteNodesInRelationships() && deleteFreeNodes()

  }

  def deleteNodesInRelationships()
                                (implicit neo4jConnection: Neo4jConnection, ec: ExecutionContext): Boolean = {

    val deletedRelationships = Cypher("MATCH (n)-[r]-(m) DELETE n, r, m").execute()
    if (deletedRelationships) {
      logger.info("Neo4j clean up: deleted nodes in a relationship (including relationships)")
      true
    } else {
      logger.error("failed to delete nodes in a relationship (and the relationships)")
      false
    }

  }

  def deleteFreeNodes()
                     (implicit neo4jConnection: Neo4jConnection, ec: ExecutionContext): Boolean = {

    val deletedFreeNodes = Cypher("MATCH (n) DELETE n").execute()
    if (deletedFreeNodes) {
      logger.info("Neo4j clean up: deleted free nodes")
      true
    } else {
      logger.error(s"failed to delete free nodes")
      false
    }

  }

}
