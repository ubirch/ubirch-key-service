package com.ubirch.keyservice.core.manager

import com.ubirch.key.model.rest.PublicKey
import com.ubirch.keyService.testTools.data.generator.TestDataGenerator
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.util.futures.FutureUtil
import com.ubirch.util.uuid.UUIDUtil

import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-05-09
  */
class PublicKeyManagerSpec extends Neo4jSpec {

  feature("create()") {

    scenario("public key does not exist (PublicKey with all fields set)") {

      // prepare
      val publicKey = TestDataGenerator.publicKey()

      // test
      PublicKeyManager.create(publicKey) map {

        case None => fail(s"failed to create key: $publicKey")
        case Some(result: PublicKey) => result shouldBe publicKey

      }

    }

    scenario("public key exists (PublicKey with all fields set)") {

      // prepare
      val publicKey = TestDataGenerator.publicKey()
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

    scenario("public key does not exist (PublicKey with only mandatory fields set)") {

      // prepare
      val publicKey = TestDataGenerator.publicKeyMandatoryOnly()

      // test
      PublicKeyManager.create(publicKey) map {

        case None => fail(s"failed to create key: $publicKey")
        case Some(result: PublicKey) => result shouldBe publicKey

      }

    }

    scenario("public key exists (PublicKey with only mandatory fields set)") {

      // prepare
      val publicKey = TestDataGenerator.publicKeyMandatoryOnly()
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
      val hardwareId = UUIDUtil.uuidStr

      val pubKey1 = TestDataGenerator.publicKey(infoHwDeviceId = hardwareId)
      pubKey1.pubKeyInfo.validNotAfter should be('isDefined)

      val pubKey2 = TestDataGenerator.publicKey(infoHwDeviceId = hardwareId)
      pubKey2.pubKeyInfo.validNotAfter should be('isDefined)

      createKeys(pubKey1, pubKey2) flatMap {

        case false => fail("failed to create public keys during preparation")

        case true =>

          // test
          PublicKeyManager.currentlyValid(hardwareId) map { result =>

            // verify
            result shouldBe Set(pubKey1, pubKey2)

          }

      }

    }

    // TODO test case: two keys: both currently valid (without notValidAfter) --> find both

    // TODO test case: two keys: first currently valid, second not valid (notValidBefore > now) --> find first

    // TODO test case: two keys: first currently valid, second not valid (notValidAfter < now) --> find first

    // TODO test case: two keys: both currently valid (with different hardware ids) --> find first

  }

  private def createKeys(pubKeys: PublicKey*): Future[Boolean] = {

    val resultsFuture = pubKeys map {
      PublicKeyManager.create(_) map {
        case None => false
        case Some(_: PublicKey) => true
      }
    }

    FutureUtil.unfoldInnerFutures(resultsFuture.toList) map { results =>
      results.forall(b => b)
    }

  }

}
