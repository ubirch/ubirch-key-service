package com.ubirch.keyservice.cmd

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.keyservice.config.KeySvcConfigKeys
import com.ubirch.util.neo4j.utils.Neo4jDriverUtil

import org.neo4j.driver.v1.Driver

/**
  * author: cvandrei
  * since: 2017-05-10
  */
trait CmdBase extends App
  with StrictLogging {

  protected implicit val neo4jDriver: Driver = Neo4jDriverUtil.driver(KeySvcConfigKeys.neo4jConfigPrefix)

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      neo4jDriver.close()
    }
  })

  run()

  def close(): Unit = {
    System.exit(0)
  }

  def run(): Unit

}
