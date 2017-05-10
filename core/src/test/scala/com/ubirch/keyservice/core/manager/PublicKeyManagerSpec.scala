package com.ubirch.keyservice.core.manager

import com.ubirch.key.model.rest.PublicKey
import com.ubirch.keyService.testTools.data.generator.TestDataGenerator
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec

/**
  * author: cvandrei
  * since: 2017-05-09
  */
class PublicKeyManagerSpec extends Neo4jSpec {

  feature("create()") {

    scenario("public key does not exist (PublicKey with all mandatory fields set)") {

      // prepare
      val publicKey = TestDataGenerator.publicKey()

      // test
      PublicKeyManager.create(publicKey) map {

        case None => fail(s"failed to create key: $publicKey")

        case Some(result: PublicKey) => result shouldBe publicKey


      }

    }

    scenario("public key exists (PublicKey with all mandatory fields set)") {

      // prepare
      val publicKey = TestDataGenerator.publicKey()
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail(s"failed to create existing key: $publicKey")

        case Some(result: PublicKey) =>

          result shouldBe publicKey

          // test
          PublicKeyManager.create(publicKey) map { result =>

            // verify
            result shouldBe None

          }

      }

    }

  }

  feature("currentlyValid()") {
    // TODO write remaining test cases
  }

}
