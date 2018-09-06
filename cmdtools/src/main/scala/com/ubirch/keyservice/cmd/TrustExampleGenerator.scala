package com.ubirch.keyservice.cmd

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo, TrustedKey, TrustRelation}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

/**
  * author: cvandrei
  * since: 2018-09-06
  */
object TrustExampleGenerator extends App {

  override def main(args: Array[String]): Unit = {

    val (fromPublicKey, fromPrivateKey) = EccUtil.generateEccKeyPairEncoded
    val fromKeyMaterial = keyMaterial(fromPublicKey, fromPrivateKey)
    //val fromKeyMaterial = keyMaterial("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val fromKeyJson = Json4sUtil.any2String(fromKeyMaterial.publicKey).get

    val (toPublicKey, toPrivateKey) = EccUtil.generateEccKeyPairEncoded
    val toKeyMaterial = keyMaterial(toPublicKey, toPrivateKey)
    //val toKeyMaterial = keyMaterial("MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=", "MC8CAQAwCAYDK2VkCgEBBCCnZ7tKYA/dzNPqgRRe6yBb+q7cj0AvWA6FVf6nxOtGlg==")
    val toKeyJson = Json4sUtil.any2String(toKeyMaterial.publicKey).get

    println(s"====== curl calls to upload public keys")
    println(s"""(from) curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$fromKeyJson'""".stripMargin)
    println(s"""(to  ) curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$toKeyJson'""".stripMargin)

    val trustKeyFromTo = trustKey(fromKeyMaterial, toKeyMaterial)
    val trustKeyJsonFromTo = Json4sUtil.any2String(trustKeyFromTo).get

    println(s"====== curl call to have one key trust another (from --> to)")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonFromTo'""")

    val trustKeyToFrom = trustKey(toKeyMaterial, fromKeyMaterial)
    val trustKeyJsonToFrom = Json4sUtil.any2String(trustKeyToFrom).get

    println(s"====== curl call to have one key trust another (to --> from)")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonToFrom'""")

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

  private def trustKey(from: KeyMaterial, to: KeyMaterial): TrustedKey = {

    val trustRelation = TrustRelation(
      created = DateUtil.nowUTC,
      fromKey = from.publicKey.pubKeyInfo.pubKey,
      toKey = to.publicKey.pubKeyInfo.pubKey,
      validNotAfter = Some(DateUtil.nowUTC.plusMonths(3))
    )
    val trustRelationJson = Json4sUtil.any2String(trustRelation).get
    val signature = EccUtil.signPayload(from.privateKeyString, trustRelationJson)

    TrustedKey(signature, trustRelation)

  }

}

case class KeyMaterial(privateKeyString: String, publicKey: PublicKey)
