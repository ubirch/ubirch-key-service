package com.ubirch.keyservice.client.rest.cache.redis

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.key.model._
import com.ubirch.key.model.db.PublicKey
import com.ubirch.key.model.rest.{PublicKeyDelete, SignedTrustRelation, TrustedKeyResult}
import com.ubirch.keyService.testTools.data.generator.{TestDataGeneratorDb, TestDataGeneratorRest}
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.keyservice.config.KeySvcConfig
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.util.date.DateUtil
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.{Json4sUtil, MyJsonProtocol}
import com.ubirch.util.model.{JsonErrorResponse, JsonResponse}
import com.ubirch.util.redis.RedisClientUtil
import com.ubirch.util.redis.test.RedisCleanup
import com.ubirch.util.uuid.UUIDUtil
import org.apache.commons.codec.binary.Hex
import org.joda.time.DateTime
import org.json4s.native.Serialization.read
import org.scalatest.{Assertion, GivenWhenThen}
import redis.RedisClient

import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2018-09-05
  */
class KeyServiceClientRestCacheRedisSpec extends Neo4jSpec
  with RedisCleanup
  with MyJsonProtocol
  with GivenWhenThen {

  implicit val redisClient: RedisClient = RedisClientUtil.getRedisClient

  val ECDSA: String = "ecdsa-p256v1"
  val EDDSA: String = "ed25519-sha-512"

  override protected def beforeEach(): Unit = {

    super.beforeEach()
    deleteAll(configPrefix = "ubirch.redisUtil")

  }

  feature("check()") {

    scenario("check without errors") {

      // test
      KeyServiceClientRestCacheRedis.check() map {

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
      KeyServiceClientRestCacheRedis.deepCheck() map { deepCheckResponse =>

        // verify
        deepCheckResponse shouldBe DeepCheckResponse()

      }

    }

  }

  def pubKeyPOST(curveAlgorithm: String): Unit = {

    scenario("new key") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateUtil.nowUTC.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1)),
        infoAlgorithm = curveAlgorithm
      )
      val restPubKey = Json4sUtil.any2any[rest.PublicKey](publicKey)

      // test
      KeyServiceClientRestCacheRedis.pubKeyPOST(restPubKey) map { result =>

        // verify
        result shouldBe defined
        result.get shouldBe restPubKey

      }

    }

    scenario("key already exists -> Some") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateUtil.nowUTC.minusDays(1),
        infoValidNotAfter = Some(DateUtil.nowUTC.plusDays(1)),
        infoAlgorithm = curveAlgorithm
      )
      val restPubKey = Json4sUtil.any2any[rest.PublicKey](publicKey)
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(_: PublicKey)) =>

          // test
          KeyServiceClientRestCacheRedis.pubKeyPOST(restPubKey) map { result =>

            // verify
            result shouldBe Some(restPubKey)

          }

      }

    }

  }

  feature("pubKeyPOST() - ECDSA") {
    scenariosFor(pubKeyPOST(ECDSA))
  }

  feature("pubKeyPOST() - EDDSA") {
    scenariosFor(pubKeyPOST(EDDSA))
  }

  def pubKeyDELETE(curveAlgorithm: String): Unit = {
    scenario("key does not exist; valid signature --> false") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1,
        infoPubKey = pubKey1,
        infoHwDeviceId = UUIDUtil.uuidStr,
        infoAlgorithm = curveAlgorithm)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val decodedPubKey = Base64.getDecoder.decode(pubKeyString)
      val signature = Base64.getEncoder.encodeToString(privKey.sign(decodedPubKey))
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKey1,
        signature = signature
      )
      privKey.verify(decodedPubKey, Base64.getDecoder.decode(signature)) shouldBe true

      // test & verify
      KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) map (_ shouldBe false)

    }

    scenario("key does not exist; invalid signature --> false") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val privKeyB = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1,
        infoPubKey = pubKey1,
        infoHwDeviceId = UUIDUtil.uuidStr,
        infoAlgorithm = curveAlgorithm
      )

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val pubKeyDecoded = Base64.getDecoder.decode(pubKeyString)
      val signature: Array[Byte] = privKeyB.sign(pubKeyString.getBytes())
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = Base64.getEncoder.encodeToString(signature)
      )
      privKey.verify(pubKeyDecoded, signature) shouldBe false

      // test & verify
      KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) map (_ shouldBe false)
    }

    scenario("key exists; invalid signature --> true and delete key") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1,
        infoPubKey = pubKey1,
        infoHwDeviceId = UUIDUtil.uuidStr,
        infoAlgorithm = curveAlgorithm)

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val decodedPubKey = Base64.getDecoder.decode(pubKeyString)
      val signature: Array[Byte] = privKey.sign(decodedPubKey)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKey1,
        signature = Base64.getEncoder.encodeToString(signature)
      )
      privKey.verify(decodedPubKey, signature) shouldBe true

      PublicKeyManager.create(pKey1) flatMap {

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(_: PublicKey)) =>

          // test
          KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) flatMap { result =>

            // verify
            result shouldBe true
            PublicKeyManager.findByPubKey(pubKeyString) map (_ shouldBe 'empty)

          }
      }
    }

    scenario("key exists; invalid signature --> false and don't delete key") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val privKeyB = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1,
        infoPubKey = pubKey1,
        infoHwDeviceId = UUIDUtil.uuidStr,
        infoAlgorithm = curveAlgorithm
      )

      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val signature: Array[Byte] = privKeyB.sign(Base64.getDecoder.decode(pubKeyString))
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = Base64.getEncoder.encodeToString(signature)
      )
      privKey.verify(Base64.getDecoder.decode(pubKeyString), signature) shouldBe false

      PublicKeyManager.create(pKey1) flatMap {

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(_: PublicKey)) =>

          // test
          KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) flatMap { result =>

            // verify
            result shouldBe false
            PublicKeyManager.findByPubKey(pubKeyString) map (_ shouldBe Some(pKey1))

          }
      }
    }
  }

  feature("pubKeyDELETE() - ECDSA") {
    scenariosFor(pubKeyDELETE(ECDSA))
  }

  feature("pubKeyDELETE() - EDDSA") {
    scenariosFor(pubKeyDELETE(EDDSA))
  }

  def findPubKeyCached(curveAlgorithm: String): Unit = {
    scenario("key does not exist --> find nothing") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val pubKey1 = Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey))
      // test
      KeyServiceClientRestCacheRedis.findPubKeyCached(pubKey1) map { result =>

        // verify
        result shouldBe empty
      }
    }

    scenario("key exists --> Some") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoAlgorithm = curveAlgorithm,
        infoValidNotAfter = Some(DateTime.now.plusDays(1))
      )
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(pubKeyDb: PublicKey)) =>

          // test
          KeyServiceClientRestCacheRedis.findPubKeyCached(pubKey1) flatMap { result =>

            // verify
            val expected = Some(Json4sUtil.any2any[rest.PublicKey](pubKeyDb))
            result shouldBe expected
            verifyCachedKey(publicKey, expected)
          }
      }
    }

    scenario("key exists in cache (for test purposes it is different from the database copy) --> Some") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1)),
        infoAlgorithm = curveAlgorithm
      )
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail(s"failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(pubKeyDb: PublicKey)) =>

          val modifiedKey = Json4sUtil.any2any[rest.PublicKey](pubKeyDb).copy(signature = "1234_invalid_signature")
          KeyServiceClientRedisCacheUtil.cachePublicKey(Some(modifiedKey)) flatMap {

            case None => fail("failed to prepare cache during test setup")

            case Some(_) =>

              // test
              KeyServiceClientRestCacheRedis.findPubKeyCached(pubKey1) flatMap { result =>

                // verify
                val expected = Some(modifiedKey)
                result shouldBe expected
                verifyCachedKey(publicKey, expected)
              }
          }
      }
    }
  }

  feature("findPubKeyCached() - ECDSA") {
    scenariosFor(findPubKeyCached(ECDSA))
  }
  feature("findPubKeyCached() - EDDSA") {
    scenariosFor(findPubKeyCached(EDDSA))
  }

  def currentlyValidPubKeysCached(curveAlgorithm: String): Unit = {
    scenario("has no keys --> None") {

      // test
      KeyServiceClientRestCacheRedis.currentlyValidPubKeysCached("1234") map { result =>

        // verify
        result shouldBe defined
        result.get shouldBe empty

      }

    }

    scenario("has valid key(s) --> Some") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoAlgorithm = curveAlgorithm
      )
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail("failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(existingPubKey: PublicKey)) =>

          val hardwareId = existingPubKey.pubKeyInfo.hwDeviceId
          // test
          KeyServiceClientRestCacheRedis.currentlyValidPubKeysCached(hardwareId) flatMap { result =>

            // verify
            result shouldBe defined
            val actual = result.get
            val expected = Set(Json4sUtil.any2any[rest.PublicKey](existingPubKey))
            actual shouldBe expected
            verifyCachedKeySet(hardwareId, expected)

          }

      }

    }

    scenario("has valid key(s) in cache (for test purpose they differ from the database copy) --> Some") {

      // prepare
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (pubKey1, privKey1) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoAlgorithm = curveAlgorithm
      )
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail("failed to prepare public key", t)

        case Right(None) => fail("failed to prepare public key")

        case Right(Some(existingPubKey: PublicKey)) =>

          val hardwareId = existingPubKey.pubKeyInfo.hwDeviceId
          val modifiedKey = Json4sUtil.any2any[rest.PublicKey](existingPubKey).copy(signature = "1234_invalid_signature")
          KeyServiceClientRedisCacheUtil.cacheValidKeys(hardwareId, Some(Set(modifiedKey))) flatMap {

            case None => fail("failed to prepare cache during test setup")

            case Some(_) =>

              // test
              KeyServiceClientRestCacheRedis.currentlyValidPubKeysCached(hardwareId) flatMap { result =>

                // verify
                result shouldBe defined
                val actual = result.get
                val expected = Set(modifiedKey)
                actual shouldBe expected
                verifyCachedKeySet(hardwareId, expected)

              }

          }

      }

    }
  }

  feature("currentlyValidPubKeysCached() - ECDSA") {
    scenariosFor(currentlyValidPubKeysCached(ECDSA))
  }

  feature("currentlyValidPubKeysCached() - EDDSA") {
    scenariosFor(currentlyValidPubKeysCached(EDDSA))
  }

  def pubKeyTrustedGet(curveAlgorithm: String): Unit = {
    scenario("empty database --> empty") {

      // prepare
      val twoKeyPairs = TestDataGeneratorRest.generateTwoKeyPairs(curveAlgorithm)

      val findTrustedSigned = TestDataGeneratorRest.findTrustedSigned(
        sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
        sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
        minTrust = 100,
        infoAlgorithm = curveAlgorithm
      )

      // test
      KeyServiceClientRestCacheRedis.pubKeyTrustedGET(findTrustedSigned) map { result =>

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
      val keyPairsAAndB = TestDataGeneratorRest.generateTwoKeyPairs(curveAlgorithm)
      val keyPairsCAndD = TestDataGeneratorRest.generateTwoKeyPairs(curveAlgorithm)
      val keyPairsEAndF = TestDataGeneratorRest.generateTwoKeyPairs(curveAlgorithm)

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
            minTrust = signedTrustRelationAToB.trustRelation.trustLevel - 1,
            infoAlgorithm = curveAlgorithm
          )

          // test
          KeyServiceClientRestCacheRedis.pubKeyTrustedGET(findTrustedSigned) map { result =>

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

    scenario("A trusts B; B is cached --> cached copy") {

      // prepare
      val keyPairAAndB = TestDataGeneratorRest.generateTwoKeyPairs(curveAlgorithm)

      uploadPublicKeys(keyPairAAndB.publicKeys) flatMap { publicKeysUploaded =>

        val expectedUploadResult = keyPairAAndB.publicKeys.map(Some(_))
        publicKeysUploaded shouldBe expectedUploadResult

        val signedTrustRelationAToB = TestDataGeneratorRest.signedTrustRelation(keyPairAAndB.keyMaterialA, keyPairAAndB.keyMaterialB)

        uploadTrust(Set(signedTrustRelationAToB)) flatMap { persistedTrust =>

          val expectedPersistedTrust = Set(signedTrustRelationAToB) map (signedTrust => Right(signedTrust))
          persistedTrust shouldBe expectedPersistedTrust

          val findTrustedSigned = TestDataGeneratorRest.findTrustedSigned(
            sourcePublicKey = keyPairAAndB.keyMaterialA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = keyPairAAndB.keyMaterialA.privateKeyString,
            minTrust = signedTrustRelationAToB.trustRelation.trustLevel,
            infoAlgorithm = curveAlgorithm
          )

          val modifiedTrustedKeyResult = TrustedKeyResult(
            depth = 1,
            trustLevel = signedTrustRelationAToB.trustRelation.trustLevel + 10,
            publicKey = keyPairAAndB.keyMaterialA.publicKey
          )
          val expectedTrustedKeyResult = Right(Set(modifiedTrustedKeyResult))
          KeyServiceClientRedisCacheUtil.cacheTrustedKeys(findTrustedSigned, expectedTrustedKeyResult) flatMap {

            case Left(jsonError) =>

              fail(s"failed to prepare cache during test setup: jsonError=$jsonError")

            case Right(_) =>

              // test
              KeyServiceClientRestCacheRedis.pubKeyTrustedGETCached(findTrustedSigned) map { result =>

                // verify
                result shouldBe Right(Set(modifiedTrustedKeyResult))

              }
          }
        }
      }
    }
  }
  feature("pubKeyTrustedGET() - ECDSA") {
    scenariosFor(pubKeyTrustedGet(ECDSA))
  }

  feature("pubKeyTrustedGET() - EDDSA") {
    scenariosFor(pubKeyTrustedGet(EDDSA))
  }

  private def verifyCachedKey(publicKey: PublicKey, expected: Option[rest.PublicKey]): Future[Assertion] = {

    val cacheKey = CacheHelperUtil.cacheKeyPublicKey(publicKey.pubKeyInfo.pubKey)
    redisClient.get[String](cacheKey) map {

      case None =>

        fail("public key should have been cached")

      case Some(json) =>

        Some(read[rest.PublicKey](json)) shouldBe expected

    }

  }

  private def verifyCachedKeySet(hardwareId: String, expected: Set[rest.PublicKey]): Future[Assertion] = {

    val cacheKey = CacheHelperUtil.cacheKeyHardwareId(hardwareId)
    redisClient.get[String](cacheKey) map {

      case None =>

        fail("public key set should have been cached")

      case Some(json) =>

        read[Set[rest.PublicKey]](json) shouldBe expected

    }

  }

  private def uploadPublicKeys(publicKeys: Set[rest.PublicKey]): Future[Set[Option[rest.PublicKey]]] = {

    Future.sequence(publicKeys map KeyServiceClientRestCacheRedis.pubKeyPOST)

  }

  private def uploadTrust(signedTrustSet: Set[SignedTrustRelation]): Future[Set[Either[JsonErrorResponse, SignedTrustRelation]]] = {

    Future.sequence(signedTrustSet map KeyServiceClientRestCacheRedis.pubKeyTrustPOST)

  }

}
