package com.ubirch.keyservice.util.server

/**
  * author: cvandrei
  * since: 2017-03-22
  */
object RouteConstants {

  final val apiPrefix = "api"
  final val serviceName = "keyService"
  final val currentVersion = "v1"

  final val pubKey = "pubkey"

  val pathPrefix = s"/$apiPrefix/$serviceName/$currentVersion"

  val pathPubKey = s"$pathPrefix/$pubKey"

}
