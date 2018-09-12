package com.ubirch.keyservice.core.manager

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.PublicKey
import com.ubirch.keyService.testTools.data.generator.{KeyGenUtil, TestDataGeneratorDb}
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.util.json.Json4sUtil

import org.scalatest.Assertion

import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2018-09-12
  */
class TrustManagerSpec extends Neo4jSpec {

  feature("upsert()") {

    scenario("empty database --> error") {

      // prepare
      val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
      val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = keyMaterialA, to = keyMaterialB)

      // test
      TrustManager.upsert(signedTrustRelation) map {

        // verify
        case Left(e: ExpressingTrustException) =>

          e.getMessage should include("not all public keys in the trust relationship are in our database")

        case Right(_) =>

          fail("upserting the trust relationship should have failed")

      }

    }

    scenario("trustLevel < 1 --> error") {
      testWithInvalidTrustLevel(0)
    }

    scenario("trustLevel > 100 --> error") {
      testWithInvalidTrustLevel(101)
    }

    scenario("invalid signature --> error") {

      // prepare
      val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
      val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

      val signedTrustRelationPrelim = TestDataGeneratorDb.signedTrustRelation(from = keyMaterialA, to = keyMaterialB)
      val signedTrustRelation = signedTrustRelationPrelim.copy(
        trustRelation = signedTrustRelationPrelim.trustRelation.copy(
          trustLevel = signedTrustRelationPrelim.trustRelation.trustLevel + 10
        )
      )

      val trustRelationJson = Json4sUtil.any2String(signedTrustRelation.trustRelation).get
      EccUtil.validateSignature(
        publicKey = keyMaterialA.publicKey.pubKeyInfo.pubKey,
        signature = signedTrustRelation.signature,
        payload = trustRelationJson
      ) shouldBe false

      // test
      TrustManager.upsert(signedTrustRelation) map {

        // verify
        case Left(e: ExpressingTrustException) =>

          e.getMessage should be("signature verification failed")

        case Right(_) =>

          fail("upserting the trust relationship should have failed")

      }

    }

    scenario("only sourceKey exists --> error") {

      // prepare
      val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
      val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = keyMaterialA, to = keyMaterialB)

      val pubKeyDbA = Json4sUtil.any2any[PublicKey](keyMaterialA.publicKey)
      PublicKeyManager.create(pubKeyDbA) flatMap {

        case Right(None) => fail("failed during preparation")

        case Left(e: Exception) => fail("failed during preparation", e)

        case Right(_) =>

          // test
          TrustManager.upsert(signedTrustRelation) map {

            // verify
            case Left(e: ExpressingTrustException) =>

              e.getMessage should include("not all public keys in the trust relationship are in our database")

            case Right(_) =>

              fail("upserting the trust relationship should have failed")

          }

      }

    }

    scenario("only targetKey exists --> error") {

      // prepare
      val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
      val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = keyMaterialA, to = keyMaterialB)

      val pubKeyDbB = Json4sUtil.any2any[PublicKey](keyMaterialB.publicKey)
      PublicKeyManager.create(pubKeyDbB) flatMap {

        case Right(None) => fail("failed during preparation")

        case Left(e: Exception) => fail("failed during preparation", e)

        case Right(_) =>

          // test
          TrustManager.upsert(signedTrustRelation) map {

            // verify
            case Left(e: ExpressingTrustException) =>

              e.getMessage should include("not all public keys in the trust relationship are in our database")

            case Right(_) =>

              fail("upserting the trust relationship should have failed")

          }

      }

    }

    scenario("source- and targetKey exist --> create") {

      // prepare
      val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
      val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = keyMaterialA, to = keyMaterialB)

      val pubKeyDbA = Json4sUtil.any2any[PublicKey](keyMaterialA.publicKey)
      PublicKeyManager.create(pubKeyDbA) flatMap {

        case Right(None) => fail("failed during upload of publicKeyA")

        case Left(e: Exception) => fail("failed during upload of publicKeyA", e)

        case Right(_) =>

          val pubKeyDbB = Json4sUtil.any2any[PublicKey](keyMaterialB.publicKey)
          PublicKeyManager.create(pubKeyDbB) flatMap {

            case Right(None) => fail("failed during upload of publicKeyA")

            case Left(e: Exception) => fail("failed during upload of publicKeyA", e)

            case Right(_) =>

              // test
              TrustManager.upsert(signedTrustRelation) flatMap {

                // verify
                case Left(e: ExpressingTrustException) =>

                  fail("upserting the trust relationship should have been successful", e)

                case Right(result) =>

                  result shouldBe signedTrustRelation

                  val sourcePubkey = signedTrustRelation.trustRelation.sourcePublicKey
                  val targetPubkey = signedTrustRelation.trustRelation.targetPublicKey
                  TrustManager.findBySourceTarget(sourcePubKey = sourcePubkey, targetPubKey = targetPubkey) map {

                    case Left(e: FindTrustException) => fail("trust relationship should be in database", e)

                    case Right(None) => fail("trust relationship should be in database")

                    case Right(Some(trustRelationInDb)) =>

                      trustRelationInDb shouldBe signedTrustRelation

                  }

              }

          }

      }

    }

    /*
    scenario("source- and targetKey exist; trust exists; change 'trustLevel' --> update") {}

    scenario("source- and targetKey exist; trust exists; change 'created' --> update") {}

    scenario("source- and targetKey exist; trust exists; change 'validNotAfter' --> update") {}

    scenario("source- and targetKey exist; trust exists; remove 'validNotAfter' --> update") {}
    */

  }

  private def testWithInvalidTrustLevel(trustLevel: Int): Future[Assertion] = {

    // prepare
    val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
    val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

    val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = keyMaterialA, to = keyMaterialB, trustLevel = trustLevel)

    // test
    TrustManager.upsert(signedTrustRelation) map {

      // verify
      case Left(e: ExpressingTrustException) =>

        e.getMessage should be("invalid trustLevel value")

      case Right(_) =>

        fail("upserting the trust relationship should have failed")

    }

  }

}
