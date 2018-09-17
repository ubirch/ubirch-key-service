package com.ubirch.keyservice.util.server

/**
  * author: cvandrei
  * since: 2017-03-22
  */
object RouteConstants {

  final val apiPrefix = "api"
  final val serviceName = "keyService"
  final val currentVersion = "v1"

  final val check = "check"
  final val deepCheck = "deepCheck"

  final val pubKey = "pubkey"
  final val trust = "trust"
  final val mpack = "mpack"
  final val current = "current"
  final val hardwareId = "hardwareId"
  final val trusted = "trusted"

  val pathPrefix = s"/$apiPrefix/$serviceName/$currentVersion"

  val pathCheck = s"$pathPrefix/$check"
  val pathDeepCheck = s"$pathPrefix/$deepCheck"

  val pathPubKey = s"$pathPrefix/$pubKey"

  val pathPubKeyTrust = s"$pathPubKey/$trust"

  val pathPubKeyMsgPack = s"$pathPrefix/$pubKey/$mpack"

  val pathPubKeyTrusted = s"$pathPubKey/$trusted"

  private val pathPubKeyCurrentHardwareId = s"$pathPubKey/$current/$hardwareId"
  def pathPubKeyCurrentHardwareId(hardwareId: String): String = s"$pathPubKeyCurrentHardwareId/$hardwareId"

}
