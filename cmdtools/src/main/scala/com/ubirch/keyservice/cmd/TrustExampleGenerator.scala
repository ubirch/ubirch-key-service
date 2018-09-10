package com.ubirch.keyservice.cmd

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo, SignedTrustRelation, TrustRelation}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

/**
  * author: cvandrei
  * since: 2018-09-06
  */
object TrustExampleGenerator extends App {

  override def main(args: Array[String]): Unit = {

    //val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
    //val keyMaterialA = keyMaterial(publicKeyA, privateKeyA)
    val keyMaterialA = keyMaterial("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyJsonA = Json4sUtil.any2String(keyMaterialA.publicKey).get

    //val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
    //val keyMaterialB = keyMaterial(publicKeyB, privateKeyB)
    val keyMaterialB = keyMaterial("MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=", "MC8CAQAwCAYDK2VkCgEBBCCnZ7tKYA/dzNPqgRRe6yBb+q7cj0AvWA6FVf6nxOtGlg==")
    val keyJsonB = Json4sUtil.any2String(keyMaterialB.publicKey).get

    println(s"## upload public keys")
    println("# Key A")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyJsonA'""".stripMargin)
    println("# Key B")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyJsonB'""".stripMargin)

    println(s"## trusting public keys")
    val trustKeyAToB = trustKey(keyMaterialA, keyMaterialB)
    val trustKeyJsonAToB = Json4sUtil.any2String(trustKeyAToB).get

    println(s"# trust(A --> B)")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonAToB'""")

    val trustKeyBToA = trustKey(keyMaterialB, keyMaterialA)
    val trustKeyJsonBToA = Json4sUtil.any2String(trustKeyBToA).get

    println(s"# trust(B --> a)")
    println(s"""curl -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonBToA'""")

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

  private def trustKey(from: KeyMaterial, to: KeyMaterial): SignedTrustRelation = {

    val trustRelation = TrustRelation(
      created = DateUtil.nowUTC,
      sourcePublicKey = from.publicKey.pubKeyInfo.pubKey,
      targetPublicKey = to.publicKey.pubKeyInfo.pubKey,
      validNotAfter = Some(DateUtil.nowUTC.plusMonths(3))
    )
    val trustRelationJson = Json4sUtil.any2String(trustRelation).get
    val signature = EccUtil.signPayload(from.privateKeyString, trustRelationJson)

    SignedTrustRelation(signature, trustRelation)

  }

}

case class KeyMaterial(privateKeyString: String, publicKey: PublicKey)
