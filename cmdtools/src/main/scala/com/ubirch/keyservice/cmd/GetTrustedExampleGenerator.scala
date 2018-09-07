package com.ubirch.keyservice.cmd

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.rest.{GetTrustedKeys, PublicKey, PublicKeyInfo, SignedGetTrustedKeys}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

/**
  * author: cvandrei
  * since: 2018-09-07
  */
object GetTrustedExampleGenerator extends App {

  override def main(args: Array[String]): Unit = {

    //val (publicKey, privateKey) = EccUtil.generateEccKeyPairEncoded
    //val keyMaterialObject = keyMaterial(publicKey, privateKey)
    val (publicKey, privateKey) = ("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyMaterialObject = keyMaterial("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyJson = Json4sUtil.any2String(keyMaterialObject.publicKey).get

    println(s"====== curl call: upload public key")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyJson'""".stripMargin)

    val signedGetTrusted = getTrustedSigned(publicKey = publicKey, privateKey = privateKey)
    val signedGetTrustedJson = Json4sUtil.any2String(signedGetTrusted).get

    println(s"====== curl call: get trusted keys")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey/getTrusted -H "Content-Type: application/json" -d '$signedGetTrustedJson'""")

  }

  private def keyMaterial(publicKey: String, privateKey: String): KeyMaterial = {

    val info = PublicKeyInfo(
      algorithm = "ECC_ED25519",
      created = DateUtil.nowUTC.minusHours(1),
      hwDeviceId = UUIDUtil.uuidStr,
      pubKey = publicKey,
      pubKeyId = publicKey,
      validNotBefore = DateUtil.nowUTC.minusMinutes(1)
    )
    val signature = EccUtil.signPayload(privateKey, Json4sUtil.any2String(info).get)
    val publicKeyObject = PublicKey(info, signature)

    KeyMaterial(privateKey, publicKeyObject)

  }

  private def getTrustedSigned(publicKey: String, privateKey: String): SignedGetTrustedKeys = {

    val getTrusted = GetTrustedKeys(
      created = DateUtil.nowUTC,
      key = publicKey
    )
    val getTrustedJson = Json4sUtil.any2String(getTrusted).get
    val signature = EccUtil.signPayload(privateKey, getTrustedJson)

    SignedGetTrustedKeys(getTrusted, signature)

  }

}

case class KeyMaterial(privateKeyString: String, publicKey: PublicKey)
