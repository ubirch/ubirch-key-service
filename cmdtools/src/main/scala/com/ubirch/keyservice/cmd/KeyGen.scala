package com.ubirch.keyservice.cmd

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

/**
  * author: cvandrei
  * since: 2018-01-19
  */
object KeyGen extends App {

  val ECDSA: String = "ecdsa-p256v1"
  val EDDSA: String = "ed25519-sha-512"

  override def main(args: Array[String]): Unit = {

    val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(EDDSA))
    val (publicKey, privateKey) = (privKey.getRawPublicKey, privKey.getRawPrivateKey)

    val pubKeyInfo = PublicKeyInfo(
      algorithm = EDDSA,
      created = DateUtil.nowUTC,
      hwDeviceId = UUIDUtil.uuidStr,
      pubKey = publicKey,
      pubKeyId = publicKey,
      validNotBefore = DateUtil.nowUTC.minusMinutes(1)
    )
    val signature = Base64.getEncoder.encodeToString(privKey.sign(Json4sUtil.any2String(pubKeyInfo).get.getBytes))

    val pubKey = PublicKey(
      pubKeyInfo = pubKeyInfo,
      signature = signature
    )
    println(s"publicKey : $publicKey")
    println(s"privateKey: $privateKey")
    println(s"PublicKey : ${Json4sUtil.any2String(pubKey).get}")

  }

}
