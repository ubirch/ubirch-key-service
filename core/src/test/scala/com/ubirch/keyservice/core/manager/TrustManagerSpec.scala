package com.ubirch.keyservice.core.manager

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, SignedTrustRelation}
import com.ubirch.keyService.testTools.data.generator.{KeyGenUtil, KeyMaterial, TestDataGeneratorDb}
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.util.json.Json4sUtil

import org.scalatest.Assertion

import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2018-09-12
  */
class TrustManagerSpec extends Neo4jSpec {

  feature("upsert()") {

    scenario("empty database --> error") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      // test
      TrustManager.upsert(signedTrustRelation) map {

        // verify
        case Left(e: ExpressingTrustException) =>

          e.getMessage should include("not all public keys in the trust relationship are in our database")

        case Right(_) =>

          fail("upserting the trust relationship should have failed")

      }

    }

    scenario("trustLevel < 1 --> error") {
      testWithInvalidTrustLevel(0)
    }

    scenario("trustLevel > 100 --> error") {
      testWithInvalidTrustLevel(101)
    }

    scenario("invalid signature --> error") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelationPrelim = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val signedTrustRelation = signedTrustRelationPrelim.copy(
        trustRelation = signedTrustRelationPrelim.trustRelation.copy(
          trustLevel = signedTrustRelationPrelim.trustRelation.trustLevel + 10
        )
      )

      val trustRelationJson = Json4sUtil.any2String(signedTrustRelation.trustRelation).get
      EccUtil.validateSignature(
        publicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
        signature = signedTrustRelation.signature,
        payload = trustRelationJson
      ) shouldBe false

      // test
      TrustManager.upsert(signedTrustRelation) map {

        // verify
        case Left(e: ExpressingTrustException) =>

          e.getMessage should be("signature verification failed")

        case Right(_) =>

          fail("upserting the trust relationship should have failed")

      }

    }

    scenario("only sourceKey exists --> error") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      val pubKeyDbA = Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialA.publicKey)
      PublicKeyManager.create(pubKeyDbA) flatMap {

        case Right(None) => fail("failed during preparation")

        case Left(e: Exception) => fail("failed during preparation", e)

        case Right(_) =>

          // test
          TrustManager.upsert(signedTrustRelation) map {

            // verify
            case Left(e: ExpressingTrustException) =>

              e.getMessage should include("not all public keys in the trust relationship are in our database")

            case Right(_) =>

              fail("upserting the trust relationship should have failed")

          }

      }

    }

    scenario("only targetKey exists --> error") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      val pubKeyDbB = Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialB.publicKey)
      PublicKeyManager.create(pubKeyDbB) flatMap {

        case Right(None) => fail("failed during preparation")

        case Left(e: Exception) => fail("failed during preparation", e)

        case Right(_) =>

          // test
          TrustManager.upsert(signedTrustRelation) map {

            // verify
            case Left(e: ExpressingTrustException) =>

              e.getMessage should include("not all public keys in the trust relationship are in our database")

            case Right(_) =>

              fail("upserting the trust relationship should have failed")

          }

      }

    }

    scenario("source- and targetKey exist --> create") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val publicKeys = Set(
        Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialA.publicKey),
        Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialB.publicKey)
      )
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectPublicKeysPersisted = publicKeys.map { pk => Right(Some(pk)) }
        publicKeysPersisted shouldBe expectPublicKeysPersisted

        val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

        // test
        TrustManager.upsert(signedTrustRelation) flatMap { result =>

          // verify
          result shouldBe Right(signedTrustRelation)

          val sourcePubkey = signedTrustRelation.trustRelation.sourcePublicKey
          val targetPubkey = signedTrustRelation.trustRelation.targetPublicKey
          TrustManager.findBySourceTarget(sourcePubKey = sourcePubkey, targetPubKey = targetPubkey) map { trustRelationInDb =>

            trustRelationInDb shouldBe Right(Some(signedTrustRelation))

          }

        }

      }

    }

    scenario("one sourceKey trusting two different targetKeys --> create") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()
      val (publicKeyC, privateKeyC) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialC = KeyGenUtil.keyMaterial(publicKey = publicKeyC, privateKey = privateKeyC)

      val publicKeys = Set(
        Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialA.publicKey),
        Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialB.publicKey),
        Json4sUtil.any2any[PublicKey](keyMaterialC.publicKey)
      )
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectPublicKeysPersisted = publicKeys.map { pk => Right(Some(pk))}
        publicKeysPersisted shouldBe expectPublicKeysPersisted

        val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
        val signedTrustRelationAToC = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = keyMaterialC)

        // test
        TrustManager.upsert(signedTrustRelationAToB) flatMap { trustAToBPersistResult =>

          trustAToBPersistResult shouldBe Right(signedTrustRelationAToB)

          TrustManager.upsert(signedTrustRelationAToC) flatMap { trustAToCPersistResult =>

            trustAToCPersistResult shouldBe Right(signedTrustRelationAToC)

            // verify
            val sourcePubKey = signedTrustRelationAToB.trustRelation.sourcePublicKey
            val targetPubKeyB = signedTrustRelationAToB.trustRelation.targetPublicKey
            TrustManager.findBySourceTarget(sourcePubKey = sourcePubKey, targetPubKey = targetPubKeyB) flatMap { trustRelationAToBInDb =>

              trustRelationAToBInDb shouldBe Right(Some(signedTrustRelationAToB))

              val targetPubKeyC = signedTrustRelationAToC.trustRelation.targetPublicKey
              TrustManager.findBySourceTarget(sourcePubKey = sourcePubKey, targetPubKey = targetPubKeyC) map { trustRelationAToCInDb =>

                trustRelationAToCInDb shouldBe Right(Some(signedTrustRelationAToC))

              }

            }

          }

        }

      }

    }

    scenario("source- and targetKey exist; trust exists; change 'trustLevel' --> update") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(trustLevel = trustRelation1.trustLevel + 10)
      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = EccUtil.signPayload(privateKey = twoKeyPairs.privateKeyA(), payload = Json4sUtil.any2String(trustRelation2).get)
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }

    scenario("source- and targetKey exist; trust exists; change 'created' --> update") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(created = trustRelation1.created.plusMinutes(2))
      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = EccUtil.signPayload(privateKey = twoKeyPairs.privateKeyA(), payload = Json4sUtil.any2String(trustRelation2).get)
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }

    scenario("source- and targetKey exist; trust exists; change 'validNotAfter' --> update") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(validNotAfter = Some(trustRelation1.validNotAfter.get.plusMonths(6)))
      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = EccUtil.signPayload(privateKey = twoKeyPairs.privateKeyA(), payload = Json4sUtil.any2String(trustRelation2).get)
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }

    scenario("source- and targetKey exist; trust exists; remove 'validNotAfter' --> update") {

      // prepare
      val twoKeyPairs = generateTwoKeyPairs()

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(validNotAfter = None)
      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = EccUtil.signPayload(privateKey = twoKeyPairs.privateKeyA(), payload = Json4sUtil.any2String(trustRelation2).get)
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }

  }

  private def testWithInvalidTrustLevel(trustLevel: Int): Future[Assertion] = {

    // prepare
    val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
    val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

    val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = keyMaterialA, to = keyMaterialB, trustLevel = trustLevel)

    // test
    TrustManager.upsert(signedTrustRelation) map {

      // verify
      case Left(e: ExpressingTrustException) =>

        e.getMessage should be("invalid trustLevel value")

      case Right(_) =>

        fail("upserting the trust relationship should have failed")

    }

  }

  private def persistPublicKeys(publicKeys: Set[PublicKey]): Future[Set[Either[Exception, Option[PublicKey]]]] = {

    Future.sequence(publicKeys map PublicKeyManager.create)

  }

  private def testUpsertWithModifiedField(publicKeys: Set[PublicKey],
                                  signedTrustRelation1: SignedTrustRelation,
                                  signedTrustRelation2: SignedTrustRelation
                                 ): Future[Assertion] = {

    persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

      val expectPublicKeysPersisted = publicKeys.map { pk => Right(Some(pk)) }
      publicKeysPersisted shouldBe expectPublicKeysPersisted

      TrustManager.upsert(signedTrustRelation1) flatMap { upsert1 =>

        upsert1 shouldBe Right(signedTrustRelation1)

        val sourcePubkey = signedTrustRelation1.trustRelation.sourcePublicKey
        val targetPubkey = signedTrustRelation1.trustRelation.targetPublicKey
        TrustManager.findBySourceTarget(sourcePubKey = sourcePubkey, targetPubKey = targetPubkey) flatMap { trustRelationInDb1 =>

          trustRelationInDb1 shouldBe Right(Some(signedTrustRelation1))

          // test
          TrustManager.upsert(signedTrustRelation2) flatMap { upsert2 =>

            upsert2 shouldBe Right(signedTrustRelation2)

            TrustManager.findBySourceTarget(sourcePubKey = sourcePubkey, targetPubKey = targetPubkey) map {
              _ shouldBe Right(Some(signedTrustRelation2))
            }

          }

        }

      }

    }

  }

  private def generateTwoKeyPairs(): KeyMaterialAAndB = {

    val (publicKeyA, privateKeyA) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialA = KeyGenUtil.keyMaterial(publicKey = publicKeyA, privateKey = privateKeyA)
    val (publicKeyB, privateKeyB) = EccUtil.generateEccKeyPairEncoded
    val keyMaterialB = KeyGenUtil.keyMaterial(publicKey = publicKeyB, privateKey = privateKeyB)

    val publicKeys = Set(
      Json4sUtil.any2any[PublicKey](keyMaterialA.publicKey),
      Json4sUtil.any2any[PublicKey](keyMaterialB.publicKey)
    )

    KeyMaterialAAndB(
      keyMaterialA = keyMaterialA,
      keyMaterialB = keyMaterialB,
      publicKeys = publicKeys
    )

  }

}

case class KeyMaterialAAndB(keyMaterialA: KeyMaterial,
                            keyMaterialB: KeyMaterial,
                            publicKeys: Set[PublicKey]
                           ) {

  def privateKeyA(): String = keyMaterialA.privateKeyString

}
