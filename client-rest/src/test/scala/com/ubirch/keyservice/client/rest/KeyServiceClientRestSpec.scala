package com.ubirch.keyservice.client.rest

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.key.model._
import com.ubirch.key.model.db.PublicKey
import com.ubirch.key.model.rest.{PublicKeyDelete, SignedTrustRelation, TrustedKeyResult}
import com.ubirch.keyService.testTools.data.generator.{TestDataGeneratorDb, TestDataGeneratorRest}
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.keyservice.config.KeySvcConfig
import com.ubirch.keyservice.core.manager.{PublicKeyManager, TrustManager}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.{JsonErrorResponse, JsonResponse}
import com.ubirch.util.uuid.UUIDUtil
import org.apache.commons.codec.binary.Hex
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
          val goInfo = s"${KeySvcConfig.goPipelineName} / ${KeySvcConfig.goPipelineLabel} / ${KeySvcConfig.goPipelineRevision}"
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
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
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
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
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
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val decodedPubKey = Base64.getDecoder.decode(pubKeyString)
      val signature = Base64.getEncoder.encodeToString(privKey.sign(decodedPubKey))
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKey1,
        signature = signature
      )
      privKey.verify(decodedPubKey, Base64.getDecoder.decode(signature)) shouldBe true

      // test & verify
      KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) map (_ shouldBe true)

    }

    scenario("key does not exist; invalid signature --> false") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val privKeyB = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val pubKeyDecoded = Base64.getDecoder.decode(pubKeyString)
      val signature = Base64.getEncoder.encodeToString(privKeyB.sign(pubKeyString.getBytes()))
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      privKey.verify(pubKeyDecoded, Base64.getDecoder.decode(signature)) shouldBe false

      // test & verify
      KeyServiceClientRest.pubKeyDELETE(pubKeyDelete) map (_ shouldBe false)

    }

    scenario("key exists; invalid signature --> true and delete key") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val decodedPubKey: Array[Byte] = Base64.getDecoder.decode(pubKeyString)
      val signature = Base64.getEncoder.encodeToString(privKey.sign(decodedPubKey))
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKey1,
        signature = signature
      )
      privKey.verify(decodedPubKey, Base64.getDecoder.decode(signature)) shouldBe true

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
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val privKeyB = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val signature = Base64.getEncoder.encodeToString(privKeyB.sign(pubKeyString.getBytes()))
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      privKey.verify(pubKeyString.getBytes, Base64.getDecoder.decode(signature)) shouldBe false

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
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val pubKey1 = Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey))
      // test
      KeyServiceClientRest.findPubKey(pubKey1) map { result =>

        // verify
        result shouldBe 'isEmpty

      }

    }

    scenario("key exists --> find it") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
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

  feature("pubKeyTrustedGET()") {

    scenario("empty database --> empty") {

      // prepare
      val twoKeyPairs = TestDataGeneratorRest.generateTwoKeyPairs()

      val findTrustedSigned = TestDataGeneratorRest.findTrustedSigned(
        sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
        sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
        minTrust = 100
      )

      // test
      KeyServiceClientRest.pubKeyTrustedGET(findTrustedSigned) map { result =>

        // verify
        result shouldBe Right(Set.empty)

      }

    }

    scenario("trust down to depth 2 --> all trusted keys down to depth=1") {

      /* Trust Relationships (all with default trust level)
         *
         * A ---trust--> B ---trust--> E
         *   |             |
         *   |             |--trust--> F
         *   |
         *   |--trust--> C ---trust--> E
         *   |             |
         *   |             |--trust--> F
         *   |
         *   |--trust--> D ---trust--> E
         *
         * expected: keys B, C, D
         */

      // prepare
      val keyPairsAAndB = TestDataGeneratorRest.generateTwoKeyPairs()
      val keyPairsCAndD = TestDataGeneratorRest.generateTwoKeyPairs()
      val keyPairsEAndF = TestDataGeneratorRest.generateTwoKeyPairs()

      val publicKeys = keyPairsAAndB.publicKeys ++ keyPairsCAndD.publicKeys ++ keyPairsEAndF.publicKeys

      uploadPublicKeys(publicKeys) flatMap { publicKeysUploaded =>

        val expectedUploadResult = publicKeys.map(Some(_))
        publicKeysUploaded shouldBe expectedUploadResult

        val keyA = keyPairsAAndB.keyMaterialA
        val keyB = keyPairsAAndB.keyMaterialB
        val keyC = keyPairsCAndD.keyMaterialA
        val keyD = keyPairsCAndD.keyMaterialB
        val keyE = keyPairsEAndF.keyMaterialA
        val keyF = keyPairsEAndF.keyMaterialB

        val signedTrustRelationAToB = TestDataGeneratorRest.signedTrustRelation(keyA, keyB)
        val signedTrustRelationAToC = TestDataGeneratorRest.signedTrustRelation(keyA, keyC)
        val signedTrustRelationAToD = TestDataGeneratorRest.signedTrustRelation(keyA, keyD)
        val signedTrustRelationBToE = TestDataGeneratorRest.signedTrustRelation(keyB, keyE)
        val signedTrustRelationBToF = TestDataGeneratorRest.signedTrustRelation(keyB, keyF)
        val signedTrustRelationCToE = TestDataGeneratorRest.signedTrustRelation(keyC, keyE)
        val signedTrustRelationCToF = TestDataGeneratorRest.signedTrustRelation(keyC, keyF)
        val signedTrustRelationDToE = TestDataGeneratorRest.signedTrustRelation(keyD, keyE)

        val signedTrustRelations = Set(
          signedTrustRelationAToB,
          signedTrustRelationAToC,
          signedTrustRelationAToD,
          signedTrustRelationBToE,
          signedTrustRelationBToF,
          signedTrustRelationCToE,
          signedTrustRelationCToF,
          signedTrustRelationDToE
        )
        uploadTrust(signedTrustRelations) flatMap { persistedTrust =>

          val expectedPersistedTrust = signedTrustRelations map (signedTrust => Right(signedTrust))
          persistedTrust shouldBe expectedPersistedTrust

          val findTrustedSigned = TestDataGeneratorRest.findTrustedSigned(
            sourcePublicKey = keyA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = keyA.privateKeyString,
            minTrust = signedTrustRelationAToB.trustRelation.trustLevel - 1
          )

          // test
          KeyServiceClientRest.pubKeyTrustedGET(findTrustedSigned) map { result =>

            // verify
            val expectedTrustedB = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
              publicKey = keyB.publicKey
            )
            val expectedTrustedC = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToC.trustRelation.trustLevel,
              publicKey = keyC.publicKey
            )
            val expectedTrustedD = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToD.trustRelation.trustLevel,
              publicKey = keyD.publicKey
            )
            result shouldBe Right(Set(expectedTrustedB, expectedTrustedC, expectedTrustedD))

          }

        }

      }

    }

  }

  feature("pubKeyTrustedGET()") {

    scenario("key does not exist --> error") {

      // prepare
      val keyPair = TestDataGeneratorRest.generateOneKeyPair()
      val signedRevoke = TestDataGeneratorRest.signedRevoke(
        publicKey = keyPair.publicKey.pubKeyInfo.pubKey,
        privateKey = keyPair.privateKeyString
      )

      // test
      KeyServiceClientRest.pubKeyRevokePOST(signedRevoke) map {

        // verify
        case Right(_) =>

          fail("revokation should have failed")

        case Left(error: JsonErrorResponse) =>

          error.errorMessage shouldBe "unable to revoke public key if it does not exist"

      }

    }

    scenario("invalid signature --> error") {

      // prepare
      val keyPair = TestDataGeneratorRest.generateOneKeyPair()

      val now = DateUtil.nowUTC
      val signedRevoke1 = TestDataGeneratorRest.signedRevoke(
        publicKey = keyPair.publicKey.pubKeyInfo.pubKey,
        privateKey = keyPair.privateKeyString,
        created = now
      )
      val signedRevoke2 = TestDataGeneratorRest.signedRevoke(
        publicKey = keyPair.publicKey.pubKeyInfo.pubKey,
        privateKey = keyPair.privateKeyString,
        created = now.plusMinutes(1)
      )
      val withInvalidSignature = signedRevoke1.copy(signature = signedRevoke2.signature)

      // test
      KeyServiceClientRest.pubKeyRevokePOST(withInvalidSignature) map {

        // verify
        case Right(_) =>

          fail("revokation should have failed")

        case Left(error: JsonErrorResponse) =>

          error.errorMessage shouldBe "signature verification failed"

      }

    }

    scenario("key exists --> revoked key") {

      // prepare
      val keyPair = TestDataGeneratorRest.generateOneKeyPair()
      val signedRevoke = TestDataGeneratorRest.signedRevoke(
        publicKey = keyPair.publicKey.pubKeyInfo.pubKey,
        privateKey = keyPair.privateKeyString
      )
      val pubKey = keyPair.publicKey

      KeyServiceClientRest.pubKeyPOST(pubKey) flatMap { createResult =>

        createResult shouldBe Some(pubKey)

        // test
        KeyServiceClientRest.pubKeyRevokePOST(signedRevoke) map { revokeResult =>

          // verify
          val pubKeyRevoked = pubKey.copy(signedRevoke = Some(signedRevoke))
          revokeResult shouldBe Right(pubKeyRevoked)

        }

      }

    }

    scenario("key has been revoked already --> error") {

      // prepare
      val keyPair = TestDataGeneratorRest.generateOneKeyPair()
      val signedRevoke = TestDataGeneratorRest.signedRevoke(
        publicKey = keyPair.publicKey.pubKeyInfo.pubKey,
        privateKey = keyPair.privateKeyString
      )
      val pubKeyRevoked = keyPair.publicKey.copy(signedRevoke = Some(signedRevoke))

      KeyServiceClientRest.pubKeyPOST(pubKeyRevoked) flatMap { createResult =>

        createResult shouldBe Some(pubKeyRevoked)

        // test
        KeyServiceClientRest.pubKeyRevokePOST(signedRevoke) map {

          // verify
          case Right(_) =>

            fail("revokation should have failed")

          case Left(error: JsonErrorResponse) =>

            error.errorMessage shouldBe "unable to revoke public key if it has been revoked already"

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
      val privKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
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

    scenario("key would be valid if not having been revoked --> find nothing") {

      // prepare
      val keyPair = TestDataGeneratorRest.generateOneKeyPair()
      val pubKey = keyPair.publicKey
      val signedRevoke = TestDataGeneratorRest.signedRevoke(
        publicKey = keyPair.publicKey.pubKeyInfo.pubKey,
        privateKey = keyPair.privateKeyString
      )
      val pubKeyRevoked = pubKey.copy(signedRevoke = Some(signedRevoke))

      KeyServiceClientRest.pubKeyPOST(pubKeyRevoked) flatMap { keyUploadResult =>

        keyUploadResult shouldBe Some(pubKeyRevoked)

        // test
        KeyServiceClientRest.currentlyValidPubKeys(pubKeyRevoked.pubKeyInfo.hwDeviceId) map { result =>

          // verify
          result shouldBe Some(Set.empty)

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

  private def uploadTrust(signedTrustSet: Set[SignedTrustRelation]): Future[Set[Either[JsonErrorResponse, SignedTrustRelation]]] = {

    Future.sequence(signedTrustSet map KeyServiceClientRest.pubKeyTrustPOST)

  }

}
