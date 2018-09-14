package com.ubirch.keyservice.cmd

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.keyService.testTools.data.generator.{KeyGenUtil, TestDataGeneratorRest}
import com.ubirch.util.json.Json4sUtil

/**
  * author: cvandrei
  * since: 2018-09-07
  */
object FindTrustedExampleGenerator extends App {

  override def main(args: Array[String]): Unit = {

    // ****** UPLOADING KEYS ******/

    //val (publicKey, privateKey) = EccUtil.generateEccKeyPairEncoded
    val (publicKeyA, privateKeyA) = ("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKeyA, privateKeyA)
    val keyAJson = Json4sUtil.any2String(keyMaterialA.publicKey).get

    val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKeyB, privateKeyB)
    val keyBJson = Json4sUtil.any2String(keyMaterialB.publicKey).get

    println(s"###### upload public keys")
    println("# Key A")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyAJson'""".stripMargin)
    println("# Key B")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyBJson'""".stripMargin)

    // ****** SET TRUST ******/

    val trustKeyAToBLevel50 = TestDataGeneratorRest.signedTrustRelation(keyMaterialA, keyMaterialB, 50)
    val trustKeyJsonAToBLevel50 = Json4sUtil.any2String(trustKeyAToBLevel50).get

    println()
    println(s"###### trust keys")
    println(s"# trust(A --trustLevel:50--> B)")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonAToBLevel50'""")

    // ****** FIND TRUSTED KEYS ******/

    val signedGetTrusted = TestDataGeneratorRest.findTrustedSigned(publicKey = publicKeyA, privateKey = privateKeyA)
    val signedGetTrustedJson = Json4sUtil.any2String(signedGetTrusted).get

    println()
    println(s"###### find keys trusted by A")
    println(s"""curl -i -XGET localhost:8095/api/keyService/v1/pubkey/trusted -H "Content-Type: application/json" -d '$signedGetTrustedJson'""")

  }

}
