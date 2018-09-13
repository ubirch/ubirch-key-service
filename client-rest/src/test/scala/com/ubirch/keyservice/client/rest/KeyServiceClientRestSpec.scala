package com.ubirch.keyservice.client.rest

import java.util.Base64

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model._
import com.ubirch.key.model.db.PublicKey
import com.ubirch.key.model.rest.{PublicKeyDelete, SignedTrustRelation}
import com.ubirch.keyService.testTools.data.generator.{TestDataGeneratorDb, TestDataGeneratorRest}
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.keyservice.config.KeyConfig
import com.ubirch.keyservice.core.manager.{PublicKeyManager, TrustManager}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.{JsonErrorResponse, JsonResponse}
import com.ubirch.util.uuid.UUIDUtil

import org.joda.time.DateTime

import scala.concurrent.Future

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
          val goInfo = s"${KeyConfig.goPipelineName} / ${KeyConfig.goPipelineLabel} / ${KeyConfig.goPipelineRevision}"
          val expected = JsonResponse(message = s"Welcome to the ubirchKeyService ( $goInfo )")
          jsonResponse shouldBe expected

      }

    }

  }

  feature("deepCheck()") {

    scenario("check without errors") {

      // test
      KeyServiceClientRest.deepCheck() map { deepCheckResponse =>

        // verify
        deepCheckResponse shouldBe DeepCheckResponse()

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
        result shouldBe defined
        result.get shouldBe restPubKey

      }

    }

    scenario("key already exists -> Some") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateUtil.nowUTC.minusDays(1),
        infoValidNotAfter = Some(DateUtil.nowUTC.plusDays(1))
      )
      val restPubKey = Json4sUtil.any2any[rest.PublicKey](publicKey)
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(_: PublicKey)) =>

          // test
          KeyServiceClientRest.pubKeyPOST(restPubKey) map { result =>

            // verify
            result shouldBe Some(restPubKey)

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
      val decodedPubKey = Base64.getDecoder.decode(pubKeyString)
      val signature = EccUtil.signPayload(privKey1, decodedPubKey)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKey1,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, decodedPubKey) shouldBe true

      // test & verify
      KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) map (_ shouldBe true)

    }

    scenario("key does not exist; invalid signature --> false") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (_, privKey2) = EccUtil.generateEccKeyPairEncoded

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val pubKeyDecoded = Base64.getDecoder.decode(pubKeyString)
      val signature = EccUtil.signPayload(privKey2, pubKeyString)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, pubKeyDecoded) shouldBe false

      // test & verify
      KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) map (_ shouldBe false)

    }

    scenario("key exists; invalid signature --> true and delete key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val decodedPubKey = Base64.getDecoder.decode(pubKeyString)
      val signature = EccUtil.signPayload(privKey1, decodedPubKey)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKey1,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, decodedPubKey) shouldBe true

      PublicKeyManager.create(pKey1) flatMap {

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(_: PublicKey)) =>

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

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(_: PublicKey)) =>

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

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(pubKeyDb: PublicKey)) =>

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
        result shouldBe defined
        result.get shouldBe empty

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

        case Left(t) => fail("failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(existingPubKey: PublicKey)) =>

          // test
          KeyServiceClientRest.currentlyValidPubKeys(existingPubKey.pubKeyInfo.hwDeviceId) map { result =>

            // verify
            result shouldBe defined
            val actual = result.get
            val expected = Set(Json4sUtil.any2any[rest.PublicKey](existingPubKey))
            actual shouldBe expected

          }

      }

    }

  }

  feature("pubKeyTrustPOST()") {

    scenario("empty database --> error") {

      // prepare
      val twoKeyPairs = TestDataGeneratorRest.generateTwoKeyPairs()

      val signedTrustRelation = TestDataGeneratorRest.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      // test
      KeyServiceClientRest.pubKeyTrustPOST(signedTrustRelation) flatMap { result =>

        result shouldBe Left(JsonErrorResponse(errorType = "TrustError", errorMessage = "it seems not all public keys in the trust relationship are in our database. are you sure all of them have been uploaded?"))

        TrustManager.findBySourceTarget(
          sourcePubKey = signedTrustRelation.trustRelation.sourcePublicKey,
          targetPubKey = signedTrustRelation.trustRelation.targetPublicKey
        ) map { inDatabase =>

          inDatabase shouldBe Right(None)

        }

      }

    }

    scenario("both keys exist --> create") {

      // prepare
      val twoKeyPairs = TestDataGeneratorRest.generateTwoKeyPairs()

      val signedTrustRelation = TestDataGeneratorRest.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      uploadPublicKeys(twoKeyPairs.publicKeys) flatMap { publicKeysUploaded =>

        val expectedUploadResult = twoKeyPairs.publicKeys.map(Some(_))
        publicKeysUploaded shouldBe expectedUploadResult

        // test
        KeyServiceClientRest.pubKeyTrustPOST(signedTrustRelation) flatMap { result =>

          result shouldBe Right(signedTrustRelation)

          TrustManager.findBySourceTarget(
            sourcePubKey = signedTrustRelation.trustRelation.sourcePublicKey,
            targetPubKey = signedTrustRelation.trustRelation.targetPublicKey
          ) map {

            case Left(_) => fail("trust should be in database")

            case Right(trustInDb) =>

              val dbObjectToRest = Json4sUtil.any2any[SignedTrustRelation](trustInDb)
              dbObjectToRest shouldBe signedTrustRelation

          }

        }

      }

    }

  }

  private def uploadPublicKeys(publicKeys: Set[rest.PublicKey]): Future[Set[Option[rest.PublicKey]]] = {

    Future.sequence(publicKeys map KeyServiceClientRest.pubKeyPOST)

  }

}
