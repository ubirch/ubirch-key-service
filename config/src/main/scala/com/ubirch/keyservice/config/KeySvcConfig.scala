package com.ubirch.keyservice.config

import com.ubirch.util.config.ConfigBase

/**
  * author: cvandrei
  * since: 2017-01-19
  */
object KeySvcConfig extends ConfigBase {

  /**
    * The interface the server runs on.
    *
    * @return interface
    */
  def interface: String = config.getString(KeySvcConfigKeys.INTERFACE)

  /**
    * Port the server listens on.
    *
    * @return port number
    */
  def port: Int = config.getInt(KeySvcConfigKeys.PORT)

  /**
    * Default server timeout.
    *
    * @return timeout in seconds
    */
  def timeout: Int = config.getInt(KeySvcConfigKeys.TIMEOUT)
  def goPipelineName: String = config.getString(KeySvcConfigKeys.GO_PIPELINE_NAME)

  def goPipelineLabel: String = config.getString(KeySvcConfigKeys.GO_PIPELINE_LABEL)
  def goPipelineRevision: String = config.getString(KeySvcConfigKeys.GO_PIPELINE_REVISION)

  /**
    * @return the maximum depth allowed for searches of trusted keys
    */
  def searchTrustedKeysMaxDepth: Int = config.getInt(KeySvcConfigKeys.SEARCH_TRUSTED_MAX_DEPTH)

  /*
   * Akka
   ************************************************************************************************/

  /**
    * Default actor timeout.
    *
    * @return timeout in seconds
    */
  def actorTimeout: Int = config.getInt(KeySvcConfigKeys.ACTOR_TIMEOUT)

  def akkaNumberOfWorkers: Int = config.getInt(KeySvcConfigKeys.AKKA_NUMBER_OF_WORKERS)

}
