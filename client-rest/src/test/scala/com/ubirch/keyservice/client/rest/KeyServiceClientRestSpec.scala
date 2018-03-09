package com.ubirch.keyservice.client.rest

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model._
import com.ubirch.key.model.db.PublicKey
import com.ubirch.key.model.rest.PublicKeyDelete
import com.ubirch.keyService.testTools.data.generator.TestDataGeneratorDb
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonResponse
import com.ubirch.util.uuid.UUIDUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2017-06-20
  */
class KeyServiceClientRestSpec extends Neo4jSpec {

  feature("check()") {

    scenario("check without errors") {

      // test
      KeyServiceClientRest.check() map {

        // verify
        case None => fail("expected a result other than None")

        case Some(jsonResponse: JsonResponse) =>
          val goInfo = s"${Config.goPipelineName} / ${Config.goPipelineLabel} / ${Config.goPipelineRevision}"
          val expected = JsonResponse(message = s"Welcome to the ubirchKeyService ( $goInfo )")
          jsonResponse should be(expected)

      }

    }

  }

  feature("deepCheck()") {

    scenario("check without errors") {

      // test
      KeyServiceClientRest.deepCheck() map { deepCheckResponse =>

        // verify
        deepCheckResponse should be(DeepCheckResponse())

      }

    }

  }

  feature("pubKeyPOST()") {

    scenario("new key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1))
      )
      val restPubKey = Json4sUtil.any2any[rest.PublicKey](publicKey)

      // test
      KeyServiceClientRest.pubKeyPOST(restPubKey) map { result =>

        // verify
        result should be('isDefined)
        result.get should be(restPubKey)

      }

    }

    scenario("key already exists") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1))
      )
      val restPubKey = Json4sUtil.any2any[rest.PublicKey](publicKey)
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail("failed to prepare public key")

        case Some(_: PublicKey) =>

          // test
          KeyServiceClientRest.pubKeyPOST(restPubKey) map { result =>

            // verify
            result should be('isEmpty)

          }

      }

    }

  }

  feature("pubKeyDELETE()") {

    scenario("key does not exist; valid signature --> true") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val signature = EccUtil.signPayload(privKey1, pubKeyString)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe true

      // test & verify
      KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) map (_ shouldBe true)

    }

    scenario("key does not exist; invalid signature --> false") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (_, privKey2) = EccUtil.generateEccKeyPairEncoded

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val signature = EccUtil.signPayload(privKey2, pubKeyString)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe false

      // test & verify
      KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) map (_ shouldBe false)

    }

    scenario("key exists; invalid signature --> true and delete key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val signature = EccUtil.signPayload(privKey1, pubKeyString)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe true

      PublicKeyManager.create(pKey1) flatMap {

        case None => fail("failed to prepare public key")

        case Some(_: PublicKey) =>

          // test
          KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) flatMap { result =>

            // verify
            result shouldBe true
            PublicKeyManager.findByPubKey(pubKeyString) map (_ shouldBe 'empty)

          }

      }

    }

    scenario("key exists; invalid signature --> false and don't delete key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (_, privKey2) = EccUtil.generateEccKeyPairEncoded
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val signature = EccUtil.signPayload(privKey2, pubKeyString)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe false

      PublicKeyManager.create(pKey1) flatMap {

        case None => fail("failed to prepare public key")

        case Some(_: PublicKey) =>

          // test
          KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) flatMap { result =>

            // verify
            result shouldBe false
            PublicKeyManager.findByPubKey(pubKeyString) map (_ shouldBe Some(pKey1))

          }

      }

    }

  }

  feature("findPubKey()") {

    scenario("key does not exist --> find nothing") {

      // prepare
      val (pubKey1, _) = EccUtil.generateEccKeyPairEncoded

      // test
      KeyServiceClientRest.findPubKey(pubKey1) map { result =>

        // verify
        result shouldBe 'isEmpty

      }

    }

    scenario("key exists --> find it") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1))
      )
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail("failed to prepare public key")

        case Some(pubKeyDb: PublicKey) =>

          // test
          KeyServiceClientRest.findPubKey(pubKey1) map { result =>

            // verify
            val expected = Some(Json4sUtil.any2any[rest.PublicKey](pubKeyDb))
            result shouldBe expected

          }

      }

    }

  }

  feature("currentlyValidPubKeys()") {

    scenario("has no keys") {

      // test
      KeyServiceClientRest.currentlyValidPubKeys("1234") map { result =>

        // verify
        result should be('isDefined)
        result.get should be('isEmpty)

      }

    }

    scenario("has valid key(s)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1)
      )
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail("failed to prepare public key")

        case Some(existingPubKey: PublicKey) =>

          // test
          KeyServiceClientRest.currentlyValidPubKeys(existingPubKey.pubKeyInfo.hwDeviceId) map { result =>

            // verify
            result should be('isDefined)
            val actual = result.get map Json4sUtil.any2any[PublicKey]
            val expected = Set(Json4sUtil.any2any[PublicKey](existingPubKey))
            actual should be(expected)

          }

      }

    }

  }

}
