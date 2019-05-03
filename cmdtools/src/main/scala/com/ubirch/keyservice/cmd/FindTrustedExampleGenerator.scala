package com.ubirch.keyservice.cmd

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.keyService.testTools.data.generator.{KeyGenUtil, TestDataGeneratorRest}
import com.ubirch.util.json.Json4sUtil

/**
  * author: cvandrei
  * since: 2018-09-07
  */
object FindTrustedExampleGenerator extends App {

  override def main(args: Array[String]): Unit = {

    // ****** UPLOADING KEYS ******/

    val (publicKeyA, privateKeyA) = ("MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", "MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==")
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKeyA, privateKeyA)
    val keyAJson = Json4sUtil.any2String(keyMaterialA.publicKey).get

    val privKeyB = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val (publicKeyB, privateKeyB) = (privKeyB.getRawPublicKey, privKeyB.getRawPrivateKey)
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKeyB, privateKeyB)
    val keyBJson = Json4sUtil.any2String(keyMaterialB.publicKey).get

    val privKeyC = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val (publicKeyC, privateKeyC) = (privKeyC.getRawPublicKey, privKeyC.getRawPrivateKey)
    val keyMaterialC = KeyGenUtil.keyMaterial(publicKeyC, privateKeyC)
    val keyCJson = Json4sUtil.any2String(keyMaterialC.publicKey).get

    println(s"###### upload public keys")
    println("# Key A")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyAJson'""".stripMargin)
    println("# Key B")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyBJson'""".stripMargin)
    println("# Key C")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '$keyCJson'""".stripMargin)

    // ****** SET TRUST ******/

    val trustKeyAToBLevel50 = TestDataGeneratorRest.signedTrustRelation(keyMaterialA, keyMaterialB)
    val trustKeyJsonAToBLevel50 = Json4sUtil.any2String(trustKeyAToBLevel50).get
    val trustKeyAToCLevel70 = TestDataGeneratorRest.signedTrustRelation(keyMaterialA, keyMaterialC, 70)
    val trustKeyJsonAToCLevel70 = Json4sUtil.any2String(trustKeyAToCLevel70).get

    println()
    println(s"###### trust keys")
    println(s"# trust(A --trustLevel:50--> B)")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonAToBLevel50'""")
    println(s"# trust(A --trustLevel:70--> C)")
    println(s"""curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '$trustKeyJsonAToCLevel70'""")

    // ****** FIND TRUSTED KEYS ******/

    val signedGetTrusted = TestDataGeneratorRest.findTrustedSigned(sourcePublicKey = publicKeyA, sourcePrivateKey = privateKeyA)
    val signedGetTrustedJson = Json4sUtil.any2String(signedGetTrusted).get

    println()
    println(s"###### find keys trusted by A")
    println(s"""curl -i -XGET localhost:8095/api/keyService/v1/pubkey/trusted -H "Content-Type: application/json" -d '$signedGetTrustedJson'""")

  }

}
