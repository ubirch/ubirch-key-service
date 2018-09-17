package com.ubirch.keyservice.cmd

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.keyService.testTools.data.generator.{KeyGenUtil, TestDataGeneratorRest}
import com.ubirch.util.json.Json4sUtil

/**
  * author: cvandrei
  * since: 2018-09-06
  */
object TrustExampleGenerator extends App {

  override def main(args: Array[String]): Unit = {

    val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
    //val (publicKeyA, privateKeyA) = ("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKeyA, privateKeyA)
    val keyJsonA = Json4sUtil.any2String(keyMaterialA.publicKey).get

    val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
    //val (publicKeyB, privateKeyB) = ("MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=", "MC8CAQAwCAYDK2VkCgEBBCCnZ7tKYA/dzNPqgRRe6yBb+q7cj0AvWA6FVf6nxOtGlg==")
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKeyB, privateKeyB)
    val keyJsonB = Json4sUtil.any2String(keyMaterialB.publicKey).get

    println(s"## upload public keys")
    println("# Key A")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyJsonA'""".stripMargin)
    println("# Key B")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyJsonB'""".stripMargin)

    val trustKeyAToBLevel50 = TestDataGeneratorRest.signedTrustRelation(keyMaterialA, keyMaterialB, 50)
    val trustKeyJsonAToBLevel50 = Json4sUtil.any2String(trustKeyAToBLevel50).get

    val trustKeyAToBLevel80 = TestDataGeneratorRest.signedTrustRelation(keyMaterialA, keyMaterialB, 80)
    val trustKeyJsonAToBLevel80 = Json4sUtil.any2String(trustKeyAToBLevel80).get

    val trustKeyBToA = TestDataGeneratorRest.signedTrustRelation(keyMaterialB, keyMaterialA)
    val trustKeyJsonBToA = Json4sUtil.any2String(trustKeyBToA).get

    println(s"## trusting public keys")
    println(s"# trust(A --trustLevel:50--> B)")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonAToBLevel50'""")
    println(s"# trust(A --trustLevel:80--> B)")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonAToBLevel80'""")
    println(s"# trust(B --> A)")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonBToA'""")

  }

}
