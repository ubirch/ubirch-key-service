package com.ubirch.keyservice.core.manager

import java.util.Base64

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyDelete}
import com.ubirch.keyService.testTools.data.generator.TestDataGeneratorDb
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
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

        case Right(None) => fail(s"failed to create key: $publicKey")
        case Right(Some(result: PublicKey)) => result shouldBe publicKey
        case Left(t) => fail(s"failed to create key", t)

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

        // verify
        case Left(e) =>

          e.isInstanceOf[Exception] shouldBe true
          e.getMessage should startWith("unable to create public key if signature is invalid")

        case Right(_) =>

          fail("should have resulted in Exception")

      }

    }

    scenario("public key exists (PublicKey with all fields set)") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      // prepare
      val publicKey = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail(s"failed to create key", t)

        case Right(None) => fail(s"failed to create existing key: $publicKey")

        case Right(Some(result: PublicKey)) =>

          result shouldBe publicKey

          // test
          PublicKeyManager.create(publicKey) map {

            // verify
            case Left(e) =>

              fail("no exception should have been triggered")

            case Right(Some(result: PublicKey)) =>

              result shouldBe publicKey

            case Right(None) =>

              fail("should returned Some(publicKey)")

          }

      }

    }

    scenario("publicKey.info.pubKey already exists (PublicKey with all fields set)") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      // prepare
      val publicKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)

      PublicKeyManager.create(publicKey1) flatMap {

        case Left(t) => fail(s"failed to create key", t)

        case Right(None) => fail(s"failed to create key during preparation: $publicKey1")

        case Right(Some(result: PublicKey)) =>

          result shouldBe publicKey1

          val publicKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)
          publicKey2.pubKeyInfo.pubKey shouldBe publicKey1.pubKeyInfo.pubKey

          // test
          PublicKeyManager.create(publicKey2) map {

            // verify
            case Left(e) =>

              e.isInstanceOf[Exception] shouldBe true
              e.getMessage should startWith("unable to create publicKey if it already exists")

            case Right(_) =>

              fail("should have resulted in Exception")

          }

      }

    }

    scenario("publicKey.info.pubKeyId already exists (PublicKey with all fields set)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val publicKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)

      PublicKeyManager.create(publicKey1) flatMap {

        case Left(t) => fail(s"failed to create key", t)

        case Right(None) => fail(s"failed to create key during preparation: $publicKey1")

        case Right(Some(result: PublicKey)) =>

          result shouldBe publicKey1

          val publicKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1)
          publicKey2.pubKeyInfo.pubKeyId shouldBe publicKey1.pubKeyInfo.pubKeyId

          // test
          PublicKeyManager.create(publicKey2) map {

            // verify
            case Left(e) =>

              e.isInstanceOf[Exception] shouldBe true
              e.getMessage should startWith("unable to create publicKey if it already exists")

            case Right(_) =>

              fail("should have resulted in Exception")

          }

      }

    }

    scenario("public key does not exist (PublicKey with only mandatory fields set)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val publicKey = TestDataGeneratorDb.publicKeyMandatoryOnly(privateKey = privKey1, infoPubKey = pubKey1)

      // test
      PublicKeyManager.create(publicKey) map {

        case Left(t) => fail(s"failed to create key", t)
        case Right(None) => fail(s"failed to create key: $publicKey")
        case Right(Some(result: PublicKey)) => result shouldBe publicKey

      }

    }

    scenario("public key exists (PublicKey with only mandatory fields set)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val publicKey = TestDataGeneratorDb.publicKeyMandatoryOnly(privateKey = privKey1, infoPubKey = pubKey1)
      PublicKeyManager.create(publicKey) flatMap {

        case Left(t) => fail(s"failed to create key", t)

        case Right(None) => fail(s"failed to create existing key: $publicKey")

        case Right(Some(result: PublicKey)) =>

          result shouldBe publicKey

          // test
          PublicKeyManager.create(publicKey) map {

            case Left(e) =>

              fail("no exception should have been triggered")

            case Right(Some(result: PublicKey)) =>

              result shouldBe publicKey

            case Right(None) =>

              fail("should returned Some(publicKey)")

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

  feature("findByPubKey()") {

    scenario("database empty; pubKey doesn't exist --> None") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val hardwareId1 = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId1)

      // test & verify
      PublicKeyManager.findByPubKey(pKey1.pubKeyInfo.pubKey) map (_ shouldBe empty)

    }

    scenario("database not empty; pubKey doesn't exist --> None") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId1 = UUIDUtil.uuidStr
      val hardwareId2 = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId1)
      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = hardwareId2)

      createKeys(pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          // test & verify
          PublicKeyManager.findByPubKey(pKey1.pubKeyInfo.pubKey) map (_ shouldBe empty)

      }

    }

    scenario("database not empty; pubKey exists --> Some") {

      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded

      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId1 = UUIDUtil.uuidStr
      val hardwareId2 = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId1)
      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = hardwareId2)
      println(s"publicKey1=$pKey1")
      println(s"publicKey2=$pKey2")

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          val pubKeyString = pKey1.pubKeyInfo.pubKey

          // test & verify
          PublicKeyManager.findByPubKey(pubKeyString) map (_ shouldBe Some(pKey1))

      }

    }

  }

  feature("deleteByPubKey()") {

    scenario("database empty; pubKey doesn't exist; valid signature --> true") {

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
      PublicKeyManager.deleteByPubKey(pubKeyDelete) map (_ shouldBe true)

    }

    scenario("database empty; pubKey doesn't exist; invalid signature --> false") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (_, privKey2) = EccUtil.generateEccKeyPairEncoded
      val hardwareId1 = UUIDUtil.uuidStr

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = hardwareId1)
      val pubKeyString = pKey1.pubKeyInfo.pubKey
      val signature = EccUtil.signPayload(privKey2, pubKeyString)
      val pubKeyDelete = PublicKeyDelete(
        publicKey = pubKeyString,
        signature = signature
      )
      EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe false

      // test & verify
      PublicKeyManager.deleteByPubKey(pubKeyDelete) map (_ shouldBe false)

    }

    scenario("database not empty; pubKey doesn't exist; valid signature --> true") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)
      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = UUIDUtil.uuidStr)

      createKeys(pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          val pubKeyString = pKey1.pubKeyInfo.pubKey
          val signature = EccUtil.signPayload(privKey1, pubKeyString)
          val pubKeyDelete = PublicKeyDelete(
            publicKey = pubKeyString,
            signature = signature
          )
          EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe true

          // test
          PublicKeyManager.deleteByPubKey(pubKeyDelete) map { result =>

            // verify
            PublicKeyManager.findByPubKey(pKey1.pubKeyInfo.pubKey) map (_ shouldBe empty)
            PublicKeyManager.findByPubKey(pKey2.pubKeyInfo.pubKey) map (_ shouldBe defined)

            result shouldBe true

          }

      }

    }

    scenario("database not empty; pubKey doesn't exist; invalid signature --> false and don't delete key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)
      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = UUIDUtil.uuidStr)

      createKeys(pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          val pubKeyString = pKey1.pubKeyInfo.pubKey
          val signature = EccUtil.signPayload(privKey2, pubKeyString)
          val pubKeyDelete = PublicKeyDelete(
            publicKey = pubKeyString,
            signature = signature
          )
          EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe false

          // test
          PublicKeyManager.deleteByPubKey(pubKeyDelete) map { result =>

            // verify
            PublicKeyManager.findByPubKey(pKey1.pubKeyInfo.pubKey) map (_ shouldBe defined)
            PublicKeyManager.findByPubKey(pKey2.pubKeyInfo.pubKey) map (_ shouldBe defined)

            result shouldBe false

          }

      }

    }

    scenario("database not empty; pubKey exists; valid signature --> true and delete key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)
      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = UUIDUtil.uuidStr)

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          val pubKeyString = pKey1.pubKeyInfo.pubKey
          val signature = EccUtil.signPayload(privKey1, pubKeyString)
          val pubKeyDelete = PublicKeyDelete(
            publicKey = pubKeyString,
            signature = signature
          )
          EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe true

          // test
          PublicKeyManager.deleteByPubKey(pubKeyDelete) flatMap { result =>

            // verify
            PublicKeyManager.findByPubKey(pubKeyDelete.publicKey) map (_ shouldBe empty)
            PublicKeyManager.findByPubKey(pKey2.pubKeyInfo.pubKey) map (_ shouldBe defined)

            result shouldBe true

          }

      }

    }

    scenario("database not empty; pubKey exists; invalid signature --> false and don't delete key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val (pubKey2, privKey2) = EccUtil.generateEccKeyPairEncoded

      val pKey1 = TestDataGeneratorDb.createPublicKey(privateKey = privKey1, infoPubKey = pubKey1, infoHwDeviceId = UUIDUtil.uuidStr)
      val pKey2 = TestDataGeneratorDb.createPublicKey(privateKey = privKey2, infoPubKey = pubKey2, infoHwDeviceId = UUIDUtil.uuidStr)

      createKeys(pKey1, pKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          val pubKeyString = pKey1.pubKeyInfo.pubKey
          val signature = EccUtil.signPayload(privKey2, pubKeyString)
          val pubKeyDelete = PublicKeyDelete(
            publicKey = pubKeyString,
            signature = signature
          )
          EccUtil.validateSignature(pubKeyString, signature, pubKeyString) shouldBe false

          // test
          PublicKeyManager.deleteByPubKey(pubKeyDelete) flatMap { result =>

            // verify
            PublicKeyManager.findByPubKey(pubKeyDelete.publicKey) map (_ shouldBe defined)
            PublicKeyManager.findByPubKey(pKey2.pubKeyInfo.pubKey) map (_ shouldBe defined)

            result shouldBe false

          }

      }

    }

  }

  private def createKeys(pubKeys: PublicKey*): Future[Boolean] = {

    // TODO copy to test-tools-ext?
    val resultsFuture = pubKeys map { pubKey =>
      PublicKeyManager.create(pubKey = pubKey) map {
        case Left(t) => false
        case Right(None) => false
        case Right(Some(_: PublicKey)) => true
      }
    }

    Future.sequence(resultsFuture.toList) map { results =>
      results.forall(b => b)
    }

  }

}
