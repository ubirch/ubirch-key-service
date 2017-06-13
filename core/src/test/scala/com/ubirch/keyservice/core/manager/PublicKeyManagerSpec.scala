package com.ubirch.keyservice.core.manager

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.PublicKey
import com.ubirch.keyService.testTools.data.generator.TestDataGeneratorDb
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.util.futures.FutureUtil
import com.ubirch.util.uuid.UUIDUtil
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-05-09
  */
class PublicKeyManagerSpec extends Neo4jSpec {

  feature("create()") {

    scenario("public key does not exist (PublicKey with all fields set)") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      // prepare
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1
      )

      // test
      PublicKeyManager.create(publicKey) map {

        case None => fail(s"failed to create key: $publicKey")
        case Some(result: PublicKey) => result shouldBe publicKey

      }

    }

    scenario("invalid public key does not exist (PublicKey with all fields set)") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded

      // prepare
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1
      )

      val invalidPublicKey = publicKey.copy(pubKeyInfo = publicKey.pubKeyInfo.copy(pubKey = pubKey2))

      // test
      PublicKeyManager.create(invalidPublicKey) map {

        case None => succeed
        case Some(result: PublicKey) => fail(s"created key public key: $publicKey")

      }

    }

    scenario("public key exists (PublicKey with all fields set)") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      // prepare
      val publicKey = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail(s"failed to create existing key: $publicKey")

        case Some(result: PublicKey) =>

          result shouldBe publicKey

          // test
          PublicKeyManager.create(publicKey) map {

            // verify
            _ shouldBe None

          }

      }

    }

    scenario("publicKey.info.pubKey already exists (PublicKey with all fields set)") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      // prepare
      val publicKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)

      PublicKeyManager.create(publicKey1) flatMap {

        case None => fail(s"failed to create key during preparation: $publicKey1")

        case Some(result: PublicKey) =>

          result shouldBe publicKey1

          val publicKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)
          publicKey2.pubKeyInfo.pubKey shouldBe publicKey1.pubKeyInfo.pubKey

          // test
          PublicKeyManager.create(publicKey2) map {

            // verify
            _ shouldBe None

          }

      }

    }

    scenario("publicKey.info.pubKeyId already exists (PublicKey with all fields set)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val publicKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)

      PublicKeyManager.create(publicKey1) flatMap {

        case None => fail(s"failed to create key during preparation: $publicKey1")

        case Some(result: PublicKey) =>

          result shouldBe publicKey1

          val publicKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)
          publicKey2.pubKeyInfo.pubKeyId shouldBe publicKey1.pubKeyInfo.pubKeyId

          // test
          PublicKeyManager.create(publicKey2) map {

            // verify
            _ shouldBe None

          }

      }

    }

    scenario("public key does not exist (PublicKey with only mandatory fields set)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val publicKey = TestDataGeneratorDb.publicKeyMandatoryOnly(privateKey = privKey1, infoPubKey = pubKey1)

      // test
      PublicKeyManager.create(publicKey) map {

        case None => fail(s"failed to create key: $publicKey")
        case Some(result: PublicKey) => result shouldBe publicKey

      }

    }

    scenario("public key exists (PublicKey with only mandatory fields set)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val publicKey = TestDataGeneratorDb.publicKeyMandatoryOnly(privateKey = privKey1, infoPubKey = pubKey1)
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail(s"failed to create existing key: $publicKey")

        case Some(result: PublicKey) =>

          result shouldBe publicKey

          // test
          PublicKeyManager.create(publicKey) map {

            // verify
            _ shouldBe None

          }

      }

    }

  }

  feature("currentlyValid()") {

    scenario("two keys: both currently valid (with notValidAfter) --> find both") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId)
      pKey1.pubKeyInfo.validNotAfter should be('isDefined)

      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = hardwareId)
      pKey2.pubKeyInfo.validNotAfter should be('isDefined)

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          // test
          PublicKeyManager.currentlyValid(hardwareId) map { result =>

            // verify
            result shouldBe Set(pKey1, pKey2)

          }

      }

    }

    scenario("two keys: both currently valid (without notValidAfter) --> find both") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.publicKeyMandatoryOnly(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId)
      pKey1.pubKeyInfo.validNotAfter should be('isEmpty)

      val pKey2 = TestDataGeneratorDb.publicKeyMandatoryOnly(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = hardwareId)
      pKey2.pubKeyInfo.validNotAfter should be('isEmpty)

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          // test
          PublicKeyManager.currentlyValid(hardwareId) map { result =>

            // verify
            result shouldBe Set(pKey1, pKey2)

          }

      }

    }

    scenario("two keys: first currently valid, second not valid (validNotBefore > now) --> find first") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId)
      val pKey2 = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey2,
        infoPubKey = pubKey2,
        infoHwDeviceId = hardwareId,
        infoValidNotBefore = DateTime.now.plusDays(1)
      )

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          // test
          PublicKeyManager.currentlyValid(hardwareId) map { result =>

            // verify
            result shouldBe Set(pKey1)

          }

      }

    }

    scenario("two keys: first currently valid, second not valid (validNotAfter < now) --> find first") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId)
      val pKey2 = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey2,
        infoPubKey = pubKey2,
        infoHwDeviceId = hardwareId,
        infoValidNotAfter = Some(DateTime.now(DateTimeZone.UTC).minusMillis(100))
      )

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          // test
          PublicKeyManager.currentlyValid(hardwareId) map { result =>

            // verify
            result shouldBe Set(pKey1)

          }

      }

    }

    scenario("two keys: both currently valid (with different hardware ids) --> find first") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId1 = UUIDUtil.uuidStr
      val hardwareId2 = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId1)
      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = hardwareId2)

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          // test
          PublicKeyManager.currentlyValid(hardwareId1) map { result =>

            // verify
            result shouldBe Set(pKey1)

          }

      }

    }

  }

  private def createKeys(pubKeys: PublicKey*): Future[Boolean] = {

    // TODO copy to test-tools-ext?
    val resultsFuture = pubKeys map { pubKey =>
      PublicKeyManager.create(pubKey = pubKey) map {
        case None => false
        case Some(_: PublicKey) => true
      }
    }

    FutureUtil.unfoldInnerFutures(resultsFuture.toList) map { results =>
      results.forall(b => b)
    }

  }

}
