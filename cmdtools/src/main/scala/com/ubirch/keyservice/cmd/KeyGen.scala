package com.ubirch.keyservice.cmd

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil
import org.apache.commons.codec.binary.Hex

/**
  * author: cvandrei
  * since: 2018-01-19
  */
object KeyGen extends App {

  override def main(args: Array[String]): Unit = {

    val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val (publicKey, privateKey) = (privKey.getRawPublicKey, privKey.getRawPrivateKey)

    val pubKeyInfo = PublicKeyInfo(
      algorithm = "ECC_ED25519",
      created = DateUtil.nowUTC,
      hwDeviceId = UUIDUtil.uuidStr,
      pubKey = publicKey,
      pubKeyId = publicKey,
      validNotBefore = DateUtil.nowUTC.minusMinutes(1)
    )
    val signature = Hex.encodeHexString(privKey.sign(Json4sUtil.any2String(pubKeyInfo).get.getBytes))

    val pubKey = PublicKey(
      pubKeyInfo = pubKeyInfo,
      signature = signature
    )
    println(s"publicKey : $publicKey")
    println(s"privateKey: $privateKey")
    println(s"PublicKey : ${Json4sUtil.any2String(pubKey).get}")

  }

}
