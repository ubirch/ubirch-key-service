package com.ubirch.key.model.rest

import com.ubirch.util.date.DateUtil

import org.scalatest.{FeatureSpec, Matchers}

/**
  * author: cvandrei
  * since: 2018-09-11
  */
class TrustRelationSpec extends FeatureSpec
  with Matchers {

  feature("trustLevel") {

    scenario("defaults to 50") {

      // test
      val trustRelation = TrustRelation(
        created = DateUtil.nowUTC,
        sourcePublicKey = "sourcePublicKey",
        targetPublicKey = "targetPublicKey"
      )

      // verify
      trustRelation.trustLevel shouldBe 50

    }

  }

}
