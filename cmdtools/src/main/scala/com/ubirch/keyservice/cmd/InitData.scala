package com.ubirch.keyservice.cmd

import com.ubirch.keyservice.util.neo4j.Neo4jUtils

/**
  * author: cvandrei
  * since: 2017-05-10
  */
object InitData extends CmdBase {

  override def run(): Unit = {

    if (Neo4jUtils.createConstraints()) {
      logger.info("created Neo4j constraints")
    } else {
      logger.error("failed to create Neo4j constraints")
    }

    close()

  }

}
