package com.ubirch.keyservice.core.manager.util

import com.ubirch.key.model.db.TrustedKeyResult
import com.ubirch.keyService.testTools.data.generator.TestDataGeneratorDb
import com.ubirch.keyservice.config.KeySvcConfig
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

/**
  * author: cvandrei
  * since: 2018-10-02
  */
class TrustManagerUtilSpec extends FeatureSpec with Matchers with GivenWhenThen {

  val ECDSA: String = "ecdsa-p256v1"
  val EDDSA: String = "ed25519-sha-512"

  feature("maxWebOfTrustDepth()") {

    scenario("depth < maxDepth --> depth") {

      // prepare
      val depth = KeySvcConfig.searchTrustedKeysMaxDepth - 1

      // test && verify
      TrustManagerUtil.maxWebOfTrustDepth(depth) shouldBe depth

    }

    scenario("depth = maxDepth --> maxDepth") {

      // prepare
      val depth = KeySvcConfig.searchTrustedKeysMaxDepth

      // test && verify
      TrustManagerUtil.maxWebOfTrustDepth(depth) shouldBe depth

    }

    scenario("depth > maxDepth --> maxDepth") {

      // prepare
      val maxDepth = KeySvcConfig.searchTrustedKeysMaxDepth
      val depth = maxDepth + 1

      // test && verify
      TrustManagerUtil.maxWebOfTrustDepth(depth) shouldBe maxDepth

    }

  }

  def extractTrustedKeys(curveAlgorithm: String): Unit = {

    scenario("[] --> empty") {
      TrustManagerUtil.extractTrustedKeys(Seq.empty) shouldBe empty
    }

    scenario("[ [trust(A-100->B)], [trust(A-100->B), trust(B-100->C)], [trust(A-100->B), trust(B-100->C), trust(C-60->B)] --> Set(B:100, C:100)") {

      val keyA = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyB = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyC = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(keyA, keyB, 100)
      val signedTrustRelationBToC = TestDataGeneratorDb.signedTrustRelation(keyB, keyC, 100)
      val signedTrustRelationCToB = TestDataGeneratorDb.signedTrustRelation(keyC, keyB, 60)

      val trustPathAToB = TrustPath(
        from = keyA.publicKey,
        signedTrust = signedTrustRelationAToB,
        to = keyB.publicKey
      )
      val trustPathBToC = TrustPath(
        from = keyB.publicKey,
        signedTrust = signedTrustRelationBToC,
        to = keyC.publicKey
      )
      val trustPathCToB = TrustPath(
        from = keyC.publicKey,
        signedTrust = signedTrustRelationCToB,
        to = keyB.publicKey
      )

      val dbSegmentAToB = Seq(trustPathAToB)
      val dbSegmentAToBToC = Seq(trustPathAToB, trustPathBToC)
      val dbSegmentAToBToCToB = Seq(trustPathAToB, trustPathBToC, trustPathCToB)

      val allTrustPaths = Seq(dbSegmentAToB, dbSegmentAToBToC, dbSegmentAToBToCToB)

      // test
      val result = TrustManagerUtil.extractTrustedKeys(allTrustPaths)

      // verify
      val trustedKeyB = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
        publicKey = keyB.publicKey
      )
      val trustedKeyC = TrustedKeyResult(
        depth = 2,
        trustLevel = signedTrustRelationBToC.trustRelation.trustLevel,
        publicKey = keyC.publicKey
      )

      result shouldBe Set(trustedKeyB, trustedKeyC)
    }
  }

  feature("extractTrustedKeys() - ECDSA") {
    scenariosFor(extractTrustedKeys(ECDSA))
  }

  feature("extractTrustedKeys() - EDDSA") {
    scenariosFor(extractTrustedKeys(EDDSA))
  }

  def extractTrustedKeysFromSinglePath(curveAlgorithm: String): Unit = {
    scenario("[] --> empty") {

      TrustManagerUtil.extractTrustedKeysFromSinglePath(Seq.empty, Set.empty) shouldBe empty

    }

    scenario("[trust(A-100->B), trust(B-100->C), trust(C-60->B)]; knownTrustedKeys is empty --> Set(B:100, C:100)") {

      // prepare
      val keyA = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyB = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyC = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(keyA, keyB, 100)
      val signedTrustRelationBToC = TestDataGeneratorDb.signedTrustRelation(keyB, keyC, 100)
      val signedTrustRelationCToB = TestDataGeneratorDb.signedTrustRelation(keyC, keyB, 60)

      val trustPathAToB = TrustPath(
        from = keyA.publicKey,
        signedTrust = signedTrustRelationAToB,
        to = keyB.publicKey
      )
      val trustPathBToC = TrustPath(
        from = keyB.publicKey,
        signedTrust = signedTrustRelationBToC,
        to = keyC.publicKey
      )
      val trustPathCToB = TrustPath(
        from = keyC.publicKey,
        signedTrust = signedTrustRelationCToB,
        to = keyB.publicKey
      )

      val trustPaths = Seq(trustPathAToB, trustPathBToC, trustPathCToB)

      // test
      val result = TrustManagerUtil.extractTrustedKeysFromSinglePath(trustPaths, Set.empty)

      // verify
      val trustedKeyB = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
        publicKey = keyB.publicKey
      )
      val trustedKeyC = TrustedKeyResult(
        depth = 2,
        trustLevel = signedTrustRelationBToC.trustRelation.trustLevel,
        publicKey = keyC.publicKey
      )

      result shouldBe Set(trustedKeyB, trustedKeyC)

    }

    scenario("[trust(A-100->B), trust(B-100->C), trust(C-60->B), trust(B-60->A]; knownTrustedKeys is empty --> Set(B:100, C:100, A:60)") {

      // prepare
      val keyA = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyB = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyC = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(keyA, keyB, 100)
      val signedTrustRelationBToC = TestDataGeneratorDb.signedTrustRelation(keyB, keyC, 100)
      val signedTrustRelationCToB = TestDataGeneratorDb.signedTrustRelation(keyC, keyB, 60)
      val signedTrustRelationBToA = TestDataGeneratorDb.signedTrustRelation(keyB, keyA, 60)

      val trustPathAToB = TrustPath(
        from = keyA.publicKey,
        signedTrust = signedTrustRelationAToB,
        to = keyB.publicKey
      )
      val trustPathBToC = TrustPath(
        from = keyB.publicKey,
        signedTrust = signedTrustRelationBToC,
        to = keyC.publicKey
      )
      val trustPathCToB = TrustPath(
        from = keyC.publicKey,
        signedTrust = signedTrustRelationCToB,
        to = keyB.publicKey
      )
      val trustPathBToA = TrustPath(
        from = keyB.publicKey,
        signedTrust = signedTrustRelationBToA,
        to = keyA.publicKey
      )

      val trustPaths = Seq(trustPathAToB, trustPathBToC, trustPathCToB, trustPathBToA)

      // test
      val result = TrustManagerUtil.extractTrustedKeysFromSinglePath(trustPaths, Set.empty)

      // verify
      val trustedKeyB = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
        publicKey = keyB.publicKey
      )
      val trustedKeyC = TrustedKeyResult(
        depth = 2,
        trustLevel = signedTrustRelationBToC.trustRelation.trustLevel,
        publicKey = keyC.publicKey
      )
      /* Semantically this test case might be confusing since the result contains nodes from depths 1, 2 and 4 while a
       * node with depth 3 is missing.
       * Through the database query we already ensure that a path like the one in this test case never happens since the
       * last path segment leading back to the origin node will not be included.
       */
      val trustedKeyA = TrustedKeyResult(
        depth = 4,
        trustLevel = signedTrustRelationBToA.trustRelation.trustLevel,
        publicKey = keyA.publicKey
      )

      result shouldBe Set(trustedKeyB, trustedKeyC, trustedKeyA)

    }

    scenario("[trust(A-100->B), trust(B-100->C)]; knownTrustedKeys non-empty --> Set(B:80, C:100)") {

      // prepare
      val keyA = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyB = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val keyC = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(keyA, keyB, 100)
      val signedTrustRelationBToC = TestDataGeneratorDb.signedTrustRelation(keyB, keyC, 100)
      val signedTrustRelationCToB = TestDataGeneratorDb.signedTrustRelation(keyC, keyB, 60)

      val trustPathAToB = TrustPath(
        from = keyA.publicKey,
        signedTrust = signedTrustRelationAToB,
        to = keyB.publicKey
      )
      val trustPathBToC = TrustPath(
        from = keyB.publicKey,
        signedTrust = signedTrustRelationBToC,
        to = keyC.publicKey
      )
      val trustPathCToB = TrustPath(
        from = keyC.publicKey,
        signedTrust = signedTrustRelationCToB,
        to = keyB.publicKey
      )

      val trustPaths = Seq(trustPathAToB, trustPathBToC, trustPathCToB)

      val trustedKeyB = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelationAToB.trustRelation.trustLevel - 20,
        publicKey = keyB.publicKey
      )
      val knownTrustedKeys = Set(trustedKeyB)

      // test
      val result = TrustManagerUtil.extractTrustedKeysFromSinglePath(trustPaths, knownTrustedKeys)

      // verify
      val trustedKeyC = TrustedKeyResult(
        depth = 2,
        trustLevel = signedTrustRelationBToC.trustRelation.trustLevel,
        publicKey = keyC.publicKey
      )

      result shouldBe knownTrustedKeys + trustedKeyC

    }
  }

  feature("extractTrustedKeysFromSinglePath() - ECDSA") {
    scenariosFor(extractTrustedKeysFromSinglePath(ECDSA))
  }
  feature("extractTrustedKeysFromSinglePath() - EDDSA") {
    scenariosFor(extractTrustedKeysFromSinglePath(EDDSA))
  }

  def containsUnknownTrustedKey(curveAlgorithm: String): Unit = {
    scenario("knownTrustedKeys is empty --> true")  {

      // prepare
      val keyPairsAAndB = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
      val key1 = keyPairsAAndB.keyMaterialA
      val key2 = keyPairsAAndB.keyMaterialB
      val signedTrustRelation1To2 = TestDataGeneratorDb.signedTrustRelation(key1, key2, 100)

      val trustPath = TrustPath(
        from = key1.publicKey,
        signedTrust = signedTrustRelation1To2,
        to = key2.publicKey
      )

      // test && verify
      TrustManagerUtil.containsUnknownTrustedKey(trustPath, Set.empty) shouldBe true
    }

    scenario("knownTrustedKeys not empty; already contains key (with same trustLevel) --> false")  {

      // prepare
      val key1 = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val key2 = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelation1To2 = TestDataGeneratorDb.signedTrustRelation(key1, key2)

      val key3 = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelation1To3 = TestDataGeneratorDb.signedTrustRelation(key1, key3)

      val trustedKey2 = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelation1To2.trustRelation.trustLevel,
        publicKey = key2.publicKey
      )
      val trustedKey3 = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelation1To3.trustRelation.trustLevel,
        publicKey = key3.publicKey
      )
      val knownTrustedKeys = Set(trustedKey2, trustedKey3)

      val trustPath = TrustPath(
        from = key1.publicKey,
        signedTrust = signedTrustRelation1To2,
        to = key2.publicKey
      )

      // test && verify
      TrustManagerUtil.containsUnknownTrustedKey(trustPath, knownTrustedKeys) shouldBe false
    }

    scenario("knownTrustedKeys not empty; already contains key (with different trustLevel) --> false")  {

      // prepare
      val key1 = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val key2 = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelation1To2a = TestDataGeneratorDb.signedTrustRelation(key1, key2, 80)
      val signedTrustRelation1To2b = TestDataGeneratorDb.signedTrustRelation(key1, key2, 100)

      val key3 = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
      val signedTrustRelation1To3 = TestDataGeneratorDb.signedTrustRelation(key1, key3)

      val trustedKey2 = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelation1To2a.trustRelation.trustLevel,
        publicKey = key2.publicKey
      )
      val trustedKey3 = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelation1To3.trustRelation.trustLevel,
        publicKey = key3.publicKey
      )
      val knownTrustedKeys = Set(trustedKey2, trustedKey3)

      val trustPath = TrustPath(
        from = key1.publicKey,
        signedTrust = signedTrustRelation1To2b,
        to = key2.publicKey
      )

      // test && verify
      TrustManagerUtil.containsUnknownTrustedKey(trustPath, knownTrustedKeys) shouldBe false
    }

    scenario("knownTrustedKeys not empty; does not contain key --> true")  {

      // prepare
      val keyPairs1And2 = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
      val key1 = keyPairs1And2.keyMaterialA
      val key2 = keyPairs1And2.keyMaterialB
      val signedTrustRelation1To2 = TestDataGeneratorDb.signedTrustRelation(key1, key2)

      val keyPairs3And4 = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
      val key3 = keyPairs3And4.keyMaterialA
      val key4 = keyPairs3And4.keyMaterialB
      val signedTrustRelation1To3 = TestDataGeneratorDb.signedTrustRelation(key1, key3)
      val signedTrustRelation1To4 = TestDataGeneratorDb.signedTrustRelation(key1, key4)

      val trustedKey3 = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelation1To3.trustRelation.trustLevel,
        publicKey = key3.publicKey
      )
      val trustedKey4 = TrustedKeyResult(
        depth = 1,
        trustLevel = signedTrustRelation1To4.trustRelation.trustLevel,
        publicKey = key4.publicKey
      )
      val knownTrustedKeys = Set(trustedKey3, trustedKey4)

      val trustPath = TrustPath(
        from = key1.publicKey,
        signedTrust = signedTrustRelation1To2,
        to = key2.publicKey
      )

      // test && verify
      TrustManagerUtil.containsUnknownTrustedKey(trustPath, knownTrustedKeys) shouldBe true
    }
  }

  feature("containsUnknownTrustedKey() - ECDSA") {
    scenariosFor(containsUnknownTrustedKey(ECDSA))
  }

  feature("containsUnknownTrustedKey() - EDDSA") {
    scenariosFor(containsUnknownTrustedKey(EDDSA))
  }

}
