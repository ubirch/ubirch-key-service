package com.ubirch.keyservice.server.actor.util

import com.ubirch.keyService.testTools.data.generator.TestDataGeneratorRest

import org.scalatest.{FeatureSpec, Matchers}

/**
  * author: cvandrei
  * since: 2017-05-11
  */
class ModelUtilSpec extends FeatureSpec
  with Matchers {

  feature("withPubKeyId()") {

    scenario("with pubKeyId = Some") {

      // prepare
      val pubKey = TestDataGeneratorRest.publicKey()
      pubKey.pubKeyInfo.pubKeyId should be('isDefined)

      // test
      val result = ModelUtil.withPubKeyId(pubKey)

      // verify
      result shouldBe pubKey

    }

    scenario("with pubKeyId = None") {

      // prepare
      val pubKey = TestDataGeneratorRest.publicKeyMandatoryOnly()
      pubKey.pubKeyInfo.pubKeyId should be('isEmpty)

      // test
      val result = ModelUtil.withPubKeyId(pubKey)

      // verify
      val info = pubKey.pubKeyInfo.copy(pubKeyId = Some(pubKey.pubKeyInfo.pubKey))
      val expected = pubKey.copy(pubKeyInfo = info)
      result shouldBe expected

    }

  }

}
