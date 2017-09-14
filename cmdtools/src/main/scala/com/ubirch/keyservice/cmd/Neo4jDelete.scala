package com.ubirch.keyservice.cmd

import com.ubirch.keyservice.utils.neo4j.Neo4jUtils

/**
  * author: cvandrei
  * since: 2017-05-10
  */
object Neo4jDelete extends CmdBase {

  override def run(): Unit = {

    if (Neo4jUtils.dropAllConstraints()) {
      logger.info("dropped all Neo4j constraints")
    } else {
      logger.error("failed to drop all Neo4j constraints")
    }

    if (Neo4jUtils.dropAllIndices()) {
      logger.info("dropped all Neo4j indices")
    } else {
      logger.error("failed to drop all Neo4j indices")
    }

    if (Neo4jUtils.deleteAllNodesAndRelationships()) {
      logger.info("deleted Neo4j nodes and possible relationships")
    } else {
      logger.error("failed to delete Neo4j nodes and possible relationships")
    }

    close()

  }

}
