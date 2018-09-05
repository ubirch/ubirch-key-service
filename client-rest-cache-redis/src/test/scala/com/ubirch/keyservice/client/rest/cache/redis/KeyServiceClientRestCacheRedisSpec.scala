package com.ubirch.keyservice.client.rest.cache.redis

import java.util.Base64

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model._
import com.ubirch.key.model.db.PublicKey
import com.ubirch.key.model.rest.PublicKeyDelete
import com.ubirch.keyService.testTools.data.generator.TestDataGeneratorDb
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.keyservice.config.KeyConfig
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.util.date.DateUtil
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.{Json4sUtil, MyJsonProtocol}
import com.ubirch.util.model.JsonResponse
import com.ubirch.util.redis.RedisClientUtil
import com.ubirch.util.redis.test.RedisCleanup
import com.ubirch.util.uuid.UUIDUtil

import org.joda.time.DateTime
import org.json4s.native.Serialization.read
import org.scalatest.Assertion

import redis.RedisClient

import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2018-09-05
  */
class KeyServiceClientRestCacheRedisSpec extends Neo4jSpec
  with RedisCleanup
  with MyJsonProtocol {

  implicit val redisClient: RedisClient = RedisClientUtil.getRedisClient

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
          val goInfo = s"${KeyConfig.goPipelineName} / ${KeyConfig.goPipelineLabel} / ${KeyConfig.goPipelineRevision}"
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

  feature("pubKeyPOST()") {

    scenario("new key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateUtil.nowUTC.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1))
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
          KeyServiceClientRestCacheRedis.pubKeyPOST(restPubKey) map { result =>

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
      KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) map (_ shouldBe true)

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
      KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) map (_ shouldBe false)

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
          KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) flatMap { result =>

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
          KeyServiceClientRestCacheRedis.pubKeyDELETE(pubKeyDelete) flatMap { result =>

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
      KeyServiceClientRestCacheRedis.findPubKey(pubKey1) map { result =>

        // verify
        result shouldBe empty

      }

    }

    scenario("key exists --> Some") {

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
          KeyServiceClientRestCacheRedis.findPubKey(pubKey1) flatMap { result =>

            // verify
            val expected = Some(Json4sUtil.any2any[rest.PublicKey](pubKeyDb))
            result shouldBe expected
            verifyCachedKey(publicKey, expected)

          }

      }

    }

    scenario("key exists in cache (for test purposes it is different from the database copy) --> Some") {

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

          val modifiedKey = Json4sUtil.any2any[rest.PublicKey](pubKeyDb).copy(signature = "1234_invalid_signature")
          KeyServiceClientRedisCacheUtil.cachePublicKey(Some(modifiedKey)) flatMap {

            case None => fail("failed to prepare cache during test setup")

            case Some(_) =>

              // test
              KeyServiceClientRestCacheRedis.findPubKey(pubKey1) flatMap { result =>

                // verify
                val expected = Some(modifiedKey)
                result shouldBe expected
                verifyCachedKey(publicKey, expected)

              }

          }

      }

    }

  }

  feature("currentlyValidPubKeys()") {

    scenario("has no keys --> None") {

      // test
      KeyServiceClientRestCacheRedis.currentlyValidPubKeys("1234") map { result =>

        // verify
        result shouldBe defined
        result.get shouldBe empty

      }

    }

    scenario("has valid key(s) --> Some") {

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

          val hardwareId = existingPubKey.pubKeyInfo.hwDeviceId
          // test
          KeyServiceClientRestCacheRedis.currentlyValidPubKeys(hardwareId) flatMap { result =>

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

          val hardwareId = existingPubKey.pubKeyInfo.hwDeviceId
          val modifiedKey = Json4sUtil.any2any[rest.PublicKey](existingPubKey).copy(signature = "1234_invalid_signature")
          KeyServiceClientRedisCacheUtil.cacheValidKeys(hardwareId, Some(Set(modifiedKey))) flatMap {

            case None => fail("failed to prepare cache during test setup")

            case Some(_) =>

              // test
              KeyServiceClientRestCacheRedis.currentlyValidPubKeys(hardwareId) flatMap { result =>

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

}
