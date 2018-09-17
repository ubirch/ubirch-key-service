package com.ubirch.keyservice.core.manager

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, SignedTrustRelation, TrustedKeyResult}
import com.ubirch.keyService.testTools.data.generator.{KeyGenUtil, TestDataGeneratorDb}
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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

      persistPublicKeys(twoKeyPairs.publicKeys) flatMap { publicKeysPersisted =>

        val expectPublicKeysPersisted = twoKeyPairs.publicKeys.map { pk => Right(Some(pk)) }
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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()
      val (publicKeyC, privateKeyC) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialC = KeyGenUtil.keyMaterial(publicKey = publicKeyC, privateKey = privateKeyC)

      val publicKeys = twoKeyPairs.publicKeys ++ Set(Json4sUtil.any2any[PublicKey](keyMaterialC.publicKey))
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectPublicKeysPersisted = publicKeys.map { pk => Right(Some(pk)) }
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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

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

  feature("delete()") {

    scenario("empty database --> true") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()
      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      // test
      TrustManager.delete(signedTrustRelation) map (_ shouldBe Right(true))

    }

    scenario("non-empty database; trust relationship to delete does not exist --> true") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()
      val (publicKeyC, privateKeyC) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialC = KeyGenUtil.keyMaterial(publicKey = publicKeyC, privateKey = privateKeyC)

      val publicKeys = twoKeyPairs.publicKeys ++ Set(Json4sUtil.any2any[PublicKey](keyMaterialC.publicKey))
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectPublicKeysPersisted = publicKeys.map { pk => Right(Some(pk)) }
        publicKeysPersisted shouldBe expectPublicKeysPersisted

        val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
        val signedTrustRelationAToC = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = keyMaterialC)

        TrustManager.upsert(signedTrustRelationAToB) flatMap { signTrustAToBPersisted =>

          signTrustAToBPersisted shouldBe Right(signedTrustRelationAToB)

          // test
          TrustManager.delete(signedTrustRelationAToC) flatMap { deleteResult =>

            // verify
            deleteResult shouldBe Right(true)

            TrustManager.findBySourceTarget(
              sourcePubKey = signedTrustRelationAToB.trustRelation.sourcePublicKey,
              targetPubKey = signedTrustRelationAToB.trustRelation.targetPublicKey
            ) map (_ shouldBe Right(Some(signedTrustRelationAToB)))

          }

        }

      }

    }

    scenario("trust relationship exists --> true") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()
      val (publicKeyC, privateKeyC) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialC = KeyGenUtil.keyMaterial(publicKey = publicKeyC, privateKey = privateKeyC)

      val publicKeys = twoKeyPairs.publicKeys ++ Set(Json4sUtil.any2any[PublicKey](keyMaterialC.publicKey))
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectPublicKeysPersisted = publicKeys.map { pk => Right(Some(pk)) }
        publicKeysPersisted shouldBe expectPublicKeysPersisted

        val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

        TrustManager.upsert(signedTrustRelation) flatMap { signTrustAToBPersisted =>

          signTrustAToBPersisted shouldBe Right(signedTrustRelation)

          // test
          TrustManager.delete(signedTrustRelation) flatMap { deleteResult =>

            // verify
            deleteResult shouldBe Right(true)

            TrustManager.findBySourceTarget(
              sourcePubKey = signedTrustRelation.trustRelation.sourcePublicKey,
              targetPubKey = signedTrustRelation.trustRelation.targetPublicKey
            ) map (_ shouldBe Right(None))

          }

        }

      }

    }

  }

  feature("findTrusted()") {

    scenario("empty database --> empty") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

      val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
        sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
        sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
        minTrust = 100
      )

      // test
      TrustManager.findTrusted(findTrustedSigned) map { result =>

        // verify
        result shouldBe Right(Set.empty)

      }

    }

    scenario("no trust relationships exist --> empty") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

      val publicKeys = twoKeyPairs.publicKeys
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
        publicKeysPersisted shouldBe expectedPublicKeysPersisted

        val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
          sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
          sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
          minTrust = 100
        )

        // test
        TrustManager.findTrusted(findTrustedSigned) map { result =>

          // verify
          result shouldBe Right(Set.empty)

        }

      }

    }

    scenario("key A doesn't trust B; B trusts A --> empty") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

      val publicKeys = twoKeyPairs.publicKeys
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
        publicKeysPersisted shouldBe expectedPublicKeysPersisted

        val signedTrustRelationBToA = TestDataGeneratorDb.signedTrustRelation(twoKeyPairs.keyMaterialB, twoKeyPairs.keyMaterialA)
        TrustManager.upsert(signedTrustRelationBToA) flatMap { signResult =>

          signResult shouldBe Right(signedTrustRelationBToA)

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
            minTrust = 100
          )

          // test
          TrustManager.findTrusted(findTrustedSigned) map { result =>

            // verify
            result shouldBe Right(Set.empty)

          }

        }

      }

    }

    scenario("key A trusts B; minTrustLevel > trustLevel --> empty") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

      val publicKeys = twoKeyPairs.publicKeys
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
        publicKeysPersisted shouldBe expectedPublicKeysPersisted

        val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(twoKeyPairs.keyMaterialA, twoKeyPairs.keyMaterialB)
        TrustManager.upsert(signedTrustRelationAToB) flatMap { signResult =>

          signResult shouldBe Right(signedTrustRelationAToB)

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
            minTrust = signedTrustRelationAToB.trustRelation.trustLevel + 1
          )

          // test
          TrustManager.findTrusted(findTrustedSigned) map { result =>

            // verify
            result shouldBe Right(Set.empty)

          }

        }

      }

    }

    scenario("key A trusts B; minTrustLevel = trustLevel --> key B") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

      val publicKeys = twoKeyPairs.publicKeys
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
        publicKeysPersisted shouldBe expectedPublicKeysPersisted

        val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(twoKeyPairs.keyMaterialA, twoKeyPairs.keyMaterialB)
        TrustManager.upsert(signedTrustRelationAToB) flatMap { signResult =>

          signResult shouldBe Right(signedTrustRelationAToB)

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
            minTrust = signedTrustRelationAToB.trustRelation.trustLevel
          )

          // test
          TrustManager.findTrusted(findTrustedSigned) map { result =>

            // verify
            val expectedTrust = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
              publicKey = Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialB.publicKey)
            )
            result shouldBe Right(Set(expectedTrust))

          }

        }

      }

    }

    scenario("key A trusts B; minTrustLevel < trustLevel --> key B") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

      val publicKeys = twoKeyPairs.publicKeys
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
        publicKeysPersisted shouldBe expectedPublicKeysPersisted

        val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(twoKeyPairs.keyMaterialA, twoKeyPairs.keyMaterialB)
        TrustManager.upsert(signedTrustRelationAToB) flatMap { signResult =>

          signResult shouldBe Right(signedTrustRelationAToB)

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
            minTrust = signedTrustRelationAToB.trustRelation.trustLevel - 1
          )

          // test
          TrustManager.findTrusted(findTrustedSigned) map { result =>

            // verify
            val expectedTrust = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
              publicKey = Json4sUtil.any2any[PublicKey](twoKeyPairs.keyMaterialB.publicKey)
            )
            result shouldBe Right(Set(expectedTrust))

          }

        }

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
      val keyPairsAAndB = TestDataGeneratorDb.generateTwoKeyPairs()
      val keyPairsCAndD = TestDataGeneratorDb.generateTwoKeyPairs()
      val keyPairsEAndF = TestDataGeneratorDb.generateTwoKeyPairs()

      val publicKeys = keyPairsAAndB.publicKeys ++ keyPairsCAndD.publicKeys ++ keyPairsEAndF.publicKeys
      persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

        val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
        publicKeysPersisted shouldBe expectedPublicKeysPersisted

        val keyA = keyPairsAAndB.keyMaterialA
        val keyB = keyPairsAAndB.keyMaterialB
        val keyC = keyPairsCAndD.keyMaterialA
        val keyD = keyPairsCAndD.keyMaterialB
        val keyE = keyPairsEAndF.keyMaterialA
        val keyF = keyPairsEAndF.keyMaterialB

        val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(keyA, keyB)
        val signedTrustRelationAToC = TestDataGeneratorDb.signedTrustRelation(keyA, keyC)
        val signedTrustRelationAToD = TestDataGeneratorDb.signedTrustRelation(keyA, keyD)
        val signedTrustRelationBToE = TestDataGeneratorDb.signedTrustRelation(keyB, keyE)
        val signedTrustRelationBToF = TestDataGeneratorDb.signedTrustRelation(keyB, keyF)
        val signedTrustRelationCToE = TestDataGeneratorDb.signedTrustRelation(keyC, keyE)
        val signedTrustRelationCToF = TestDataGeneratorDb.signedTrustRelation(keyC, keyF)
        val signedTrustRelationDToE = TestDataGeneratorDb.signedTrustRelation(keyD, keyE)

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
        persistTrustSet(signedTrustRelations) flatMap { persistedTrust =>

          val expectedPersistedTrust = signedTrustRelations map (signedTrust => Right(signedTrust))
          persistedTrust shouldBe expectedPersistedTrust

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = keyA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = keyA.privateKeyString,
            minTrust = signedTrustRelationAToB.trustRelation.trustLevel - 1
          )

          // test
          TrustManager.findTrusted(findTrustedSigned) map { result =>

            // verify
            val expectedTrustedB = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
              publicKey = Json4sUtil.any2any[PublicKey](keyB.publicKey)
            )
            val expectedTrustedC = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToC.trustRelation.trustLevel,
              publicKey = Json4sUtil.any2any[PublicKey](keyC.publicKey)
            )
            val expectedTrustedD = TrustedKeyResult(
              depth = 1,
              trustLevel = signedTrustRelationAToD.trustRelation.trustLevel,
              publicKey = Json4sUtil.any2any[PublicKey](keyD.publicKey)
            )
            result shouldBe Right(Set(expectedTrustedB, expectedTrustedC, expectedTrustedD))

          }

        }

      }

    }

  }

  private def testWithInvalidTrustLevel(trustLevel: Int): Future[Assertion] = {

    // prepare
    val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs()

    val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB, trustLevel = trustLevel)

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

  private def persistTrustSet(trustSet: Set[SignedTrustRelation]): Future[Set[Either[ExpressingTrustException, SignedTrustRelation]]] = {

    Future.sequence(trustSet map TrustManager.upsert)

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

}
