package com.ubirch.keyservice.cmd

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

/**
  * author: cvandrei
  * since: 2018-01-19
  */
object KeyGen extends App {

  val (publicKey, privateKey) = EccUtil.generateEccKeyPairEncoded
  val publicKeySigned = EccUtil.signPayload(privateKey, publicKey)

  val pubKeyInfo = PublicKeyInfo(
    algorithm = "ECC_ED25519",
    created = DateUtil.nowUTC,
    hwDeviceId = UUIDUtil.uuidStr,
    pubKey = publicKey,
    pubKeyId = Some(publicKey),
    validNotBefore = DateUtil.nowUTC.minusMinutes(1)
  )
  val signature = EccUtil.signPayload(
    privateKey = privateKey,
    payload = Json4sUtil.any2String(pubKeyInfo).get
  )

  val pubKey = PublicKey(
    pubKeyInfo = pubKeyInfo,
    signature = signature
  )
  println(s"publicKey: $publicKey")
  println(s"privateKey: $privateKey")
  println(s"PublicKey: ${Json4sUtil.any2String(pubKey).get}")

}
