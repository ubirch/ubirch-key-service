package com.ubirch.keyservice.cmd

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo, Revokation, SignedRevoke}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

/**
  * author: cvandrei
  * since: 2018-09-07
  */
object RevokeExampleGenerator extends App {

  val ECDSA: String = "ecdsa-p256v1"
  val EDDSA: String = "ed25519-sha-512"

  override def main(args: Array[String]): Unit = {

    //val (publicKey, privateKey) = EccUtil.generateEccKeyPairEncoded
    //val keyMaterialObject = keyMaterial(publicKey, privateKey)
    val (publicKey, privateKey) = ("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyMaterialObject = keyMaterial("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyJson = Json4sUtil.any2String(keyMaterialObject.publicKey).get

    println(s"====== curl call: upload public key")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyJson'""".stripMargin)

    val signedRevoke = revokeKey(publicKey = publicKey, privateKey = privateKey)
    val signedRevokeJson = Json4sUtil.any2String(signedRevoke).get

    println(s"====== curl call: revoke key")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey/revoke -H "Content-Type: application/json" -d '$signedRevokeJson'""")

  }

  private def keyMaterial(publicKey: String, privateKey: String): KeyMaterial = {

    val info = PublicKeyInfo(
      algorithm = EDDSA,
      created = DateUtil.nowUTC.minusHours(1),
      hwDeviceId = UUIDUtil.uuidStr,
      pubKey = publicKey,
      pubKeyId = publicKey,
      validNotBefore = DateUtil.nowUTC.minusMinutes(1)
    )
    val privKey = GeneratorKeyFactory.getPrivKey(privateKey, associateCurve(EDDSA))
    val signature = Base64.getEncoder.encodeToString(privKey.sign(Json4sUtil.any2String(info).get.getBytes()))
    val publicKeyObject = PublicKey(info, signature)

    KeyMaterial(privateKey, publicKeyObject)

  }

  private def revokeKey(publicKey: String, privateKey: String): SignedRevoke = {

    val revoke = Revokation(
      revokationDate = DateUtil.nowUTC,
      publicKey = publicKey,
      curveAlgorithm = EDDSA
    )
    val revokeJson = Json4sUtil.any2String(revoke).get
    val privKey = GeneratorKeyFactory.getPrivKey(privateKey, associateCurve(EDDSA))
    val signature = Base64.getEncoder.encodeToString(privKey.sign(revokeJson.getBytes))

    SignedRevoke(revoke, signature)

  }

}

case class KeyMaterial(privateKeyString: String, publicKey: PublicKey)
