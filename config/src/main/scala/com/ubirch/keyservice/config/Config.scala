package com.ubirch.keyservice.config

import com.ubirch.util.config.ConfigBase

/**
  * author: cvandrei
  * since: 2017-01-19
  */
object Config extends ConfigBase {

  /**
    * The interface the server runs on.
    *
    * @return interface
    */
  def interface: String = config.getString(ConfigKeys.INTERFACE)

  /**
    * Port the server listens on.
    *
    * @return port number
    */
  def port: Int = config.getInt(ConfigKeys.PORT)

  /**
    * Default server timeout.
    *
    * @return timeout in seconds
    */
  def timeout: Int = config.getInt(ConfigKeys.TIMEOUT)

  /*
   * Akka
   ************************************************************************************************/

  /**
    * Default actor timeout.
    *
    * @return timeout in seconds
    */
  def actorTimeout: Int = config.getInt(ConfigKeys.ACTOR_TIMEOUT)

  def akkaNumberOfWorkers: Int = config.getInt(ConfigKeys.AKKA_NUMBER_OF_WORKERS)

  /*
   * Neo4j
   ************************************************************************************************/

  private def neo4jHost(): String = stringWithDefault(ConfigKeys.NEO4J_HOST, default = "localhost")

  private def neo4jPort(): Int = intWithDefault(ConfigKeys.NEO4J_PORT, default = 7474)

  private def neo4jUserName(): String = stringWithDefault(ConfigKeys.NEO4J_USER_NAME, default = "")

  private def neo4jPassword(): String = stringWithDefault(ConfigKeys.NEO4J_PASSWORD, default = "")

  private def neo4jHttps(): Boolean = booleanWithDefault(ConfigKeys.NEO4J_HTTPS, default = false)

  def neo4jConfig(): Neo4jConfig = Neo4jConfig(
    host = neo4jHost(),
    port = neo4jPort(),
    userName = neo4jUserName(),
    password = neo4jPassword(),
    https = neo4jHttps()
  )

  /*
   * Internal Utils
   ************************************************************************************************/

  private def stringWithDefault(key: String, default: String): String = {

    if (config.hasPath(key)) {
      config.getString(key)
    } else {
      default
    }

  }

  private def intWithDefault(key: String, default: Int): Int = {

    if (config.hasPath(key)) {
      config.getInt(key)
    } else {
      default
    }

  }

  private def booleanWithDefault(key: String, default: Boolean): Boolean = {

    if (config.hasPath(key)) {
      config.getBoolean(key)
    } else {
      default
    }

  }

}

case class Neo4jConfig(host: String,
                       port: Int,
                       userName: String,
                       password: String,
                       https: Boolean
                      )
