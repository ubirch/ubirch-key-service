package com.ubirch.keyservice.core.manager

import java.util.Base64

import com.ubirch.crypto.{GeneratorKeyFactory, PrivKey}
import com.ubirch.key.model.db.{PublicKey, SignedTrustRelation, TrustedKeyResult}
import com.ubirch.keyService.testTools.data.generator.{KeyGenUtil, KeyMaterialDb, TestDataGeneratorDb}
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import org.apache.commons.codec.binary.Hex
import org.scalatest.{Assertion, GivenWhenThen}

import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2018-09-12
  */
class TrustManagerSpec extends Neo4jSpec with GivenWhenThen {
  val ECDSA: String = "ecdsa-p256v1"
  val EDDSA: String = "ed25519-sha-512"

  def upsert(curveAlgorithm: String): Unit = {
    scenario("empty database --> error") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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
      testWithInvalidTrustLevel(0, curveAlgorithm)
    }

    scenario("trustLevel > 100 --> error") {
      testWithInvalidTrustLevel(101, curveAlgorithm)
    }

    scenario("invalid signature --> error") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val signedTrustRelationPrelim = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val signedTrustRelation = signedTrustRelationPrelim.copy(
        trustRelation = signedTrustRelationPrelim.trustRelation.copy(
          trustLevel = signedTrustRelationPrelim.trustRelation.trustLevel + 10
        )
      )

      val trustRelationJson = Json4sUtil.any2String(signedTrustRelation.trustRelation).get

      val pubKeyB64: Array[Byte] = Base64.getDecoder.decode(twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey)
      val pubKey = GeneratorKeyFactory.getPubKey(pubKeyB64, associateCurve(curveAlgorithm))
      pubKey.verify(trustRelationJson.getBytes, Base64.getDecoder.decode(signedTrustRelation.signature)) shouldBe false

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      val pubKeyDbA = twoKeyPairs.keyMaterialA.publicKey
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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      val pubKeyDbB = twoKeyPairs.keyMaterialB.publicKey
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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (privateKeyC, publicKeyC) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)))

      val keyMaterialC = KeyGenUtil.keyMaterialDb(publicKey = publicKeyC,
        privateKey = privateKeyC,
        algorithmCurve = curveAlgorithm)

      val publicKeys = twoKeyPairs.publicKeys ++ Set(keyMaterialC.publicKey)
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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(trustLevel = trustRelation1.trustLevel + 10)
      val privKeyB64: Array[Byte] = Base64.getDecoder.decode(twoKeyPairs.privateKeyA())
      val privKey = GeneratorKeyFactory.getPrivKey(privKeyB64, associateCurve(curveAlgorithm))
      val sign = Base64.getEncoder.encodeToString(privKey.sign(Json4sUtil.any2String(trustRelation2).get.getBytes))

      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = sign
        //signature = EccUtil.signPayload(privateKey = twoKeyPairs.privateKeyA(), payload = Json4sUtil.any2String(trustRelation2).get)
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }

    scenario("source- and targetKey exist; trust exists; change 'created' --> update") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(created = trustRelation1.created.plusMinutes(2))
      val privKeyB64: Array[Byte] = Base64.getDecoder.decode(twoKeyPairs.privateKeyA())
      val privKey = GeneratorKeyFactory.getPrivKey(privKeyB64, associateCurve(curveAlgorithm))
      val sign = Base64.getEncoder.encodeToString(privKey.sign(Json4sUtil.any2String(trustRelation2).get.getBytes))
      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = sign
        //signature = EccUtil.signPayload(privateKey = twoKeyPairs.privateKeyA(), payload = Json4sUtil.any2String(trustRelation2).get)
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }

    scenario("source- and targetKey exist; trust exists; change 'validNotAfter' --> update") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(validNotAfter = Some(trustRelation1.validNotAfter.get.plusMonths(6)))
      val privKeyB64: Array[Byte] = Base64.getDecoder.decode(twoKeyPairs.privateKeyA())
      associateCurve(curveAlgorithm)
      val privKey = GeneratorKeyFactory.getPrivKey(privKeyB64, associateCurve(curveAlgorithm))
      val sign = Base64.getEncoder.encodeToString(privKey.sign(Json4sUtil.any2String(trustRelation2).get.getBytes))
      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = sign
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }

    scenario("source- and targetKey exist; trust exists; remove 'validNotAfter' --> update") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val signedTrustRelation1 = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)
      val trustRelation1 = signedTrustRelation1.trustRelation

      val trustRelation2 = trustRelation1.copy(validNotAfter = None)
      val privKeyB64: Array[Byte] = Base64.getDecoder.decode(twoKeyPairs.privateKeyA())
      val privKey = GeneratorKeyFactory.getPrivKey(privKeyB64, associateCurve(curveAlgorithm))
      val sign = Base64.getEncoder.encodeToString(privKey.sign(Json4sUtil.any2String(trustRelation2).get.getBytes))
      val signedTrustRelation2 = SignedTrustRelation(
        trustRelation = trustRelation2,
        signature = sign
      )

      testUpsertWithModifiedField(twoKeyPairs.publicKeys, signedTrustRelation1, signedTrustRelation2)

    }
  }
  
  feature("upsert() - ECDSA") {
    scenariosFor(upsert(ECDSA))
  }

  feature("upsert() - EDDSA") {
    scenariosFor(upsert(EDDSA))
  }

  def delete(curveAlgorithm: String): Unit = {
    scenario("empty database --> true") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
      val signedTrustRelation = TestDataGeneratorDb.signedTrustRelation(from = twoKeyPairs.keyMaterialA, to = twoKeyPairs.keyMaterialB)

      // test
      TrustManager.delete(signedTrustRelation) map (_ shouldBe Right(true))
    }

    scenario("non-empty database; trust relationship to delete does not exist --> true") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
      val privKey: PrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (publicKeyC, privateKeyC) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      //      val (publicKeyC, privateKeyC) = EccUtil.generateEccKeyPairEncoded
      val keyMaterialC = KeyGenUtil.keyMaterialDb(publicKey = publicKeyC,
        privateKey = privateKeyC,
        algorithmCurve = curveAlgorithm
      )

      val publicKeys = twoKeyPairs.publicKeys ++ Set(keyMaterialC.publicKey)
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
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
      val privKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveAlgorithm))
      val (publicKeyC, privateKeyC) = (Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPublicKey)),
        Base64.getEncoder.encodeToString(Hex.decodeHex(privKey.getRawPrivateKey)))
      val keyMaterialC = KeyGenUtil.keyMaterialDb(publicKey = publicKeyC,
        privateKey = privateKeyC,
        algorithmCurve = curveAlgorithm
      )

      val publicKeys = twoKeyPairs.publicKeys ++ Set(keyMaterialC.publicKey)
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

  feature("delete() - ECDSA") {
    scenariosFor(delete(ECDSA))
  }

  feature("delete() - EDDSA") {
    scenariosFor(delete(EDDSA))
  }

  def findTrusted(curveAlgorithm: String): Unit = {
    scenario("empty database --> empty") {

      // prepare
      val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

      val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
        sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
        sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
        minTrust = 100,
        algortihmCurve = curveAlgorithm
      )

      // test
      TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

        // verify
        result shouldBe Right(Set.empty)


      }
    }

      scenario("no trust relationships exist --> empty") {

        // prepare
        val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

        val publicKeys = twoKeyPairs.publicKeys
        persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

          val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
          publicKeysPersisted shouldBe expectedPublicKeysPersisted

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = twoKeyPairs.keyMaterialA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = twoKeyPairs.keyMaterialA.privateKeyString,
            minTrust = 100,
            algortihmCurve = curveAlgorithm
          )

          // test
          TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

            // verify
            result shouldBe Right(Set.empty)

          }
        }
      }

      scenario("key A doesn't trust B; B trusts A --> empty") {

        // prepare
        val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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
              minTrust = 100,
              algortihmCurve = curveAlgorithm
            )

            // test
            TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

              // verify
              result shouldBe Right(Set.empty)

            }
          }
        }
      }

      scenario("key A trusts B; minTrustLevel > trustLevel --> empty") {

        // prepare
        val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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
              minTrust = signedTrustRelationAToB.trustRelation.trustLevel + 1,
              algortihmCurve = curveAlgorithm
            )

            // test
            TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

              // verify
              result shouldBe Right(Set.empty)

            }
          }
        }
      }

      scenario("key A trusts B trusts C; trust(A->B) has expired --> empty") {

        // prepare
        val keyA = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
        val keyB = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)
        val keyC = TestDataGeneratorDb.generateOneKeyPair(curveAlgorithm)

        val publicKeys = Set(keyA.publicKey, keyB.publicKey, keyC.publicKey)
        persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

          val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
          publicKeysPersisted shouldBe expectedPublicKeysPersisted

          val signedTrustRelationAToB = TestDataGeneratorDb.signedTrustRelation(keyA, keyB, validNotAfter = Some(DateUtil.nowUTC.minus(1)))
          val signedTrustRelationBToC = TestDataGeneratorDb.signedTrustRelation(keyB, keyC)
          TrustManager.upsert(signedTrustRelationAToB) flatMap { signAToBResult =>

            signAToBResult shouldBe Right(signedTrustRelationAToB)

            TrustManager.upsert(signedTrustRelationBToC) flatMap { signBToCResult =>

              signBToCResult shouldBe Right(signedTrustRelationBToC)

              val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
                sourcePublicKey = keyA.publicKey.pubKeyInfo.pubKey,
                sourcePrivateKey = keyA.privateKeyString,
                minTrust = signedTrustRelationAToB.trustRelation.trustLevel + 1,
                algortihmCurve = curveAlgorithm
              )

              // test
              TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

                // verify
                result shouldBe Right(Set.empty)

              }
            }
          }
        }
      }

      scenario("key A trusts B; minTrustLevel = trustLevel --> key B") {

        // prepare
        val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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
              minTrust = signedTrustRelationAToB.trustRelation.trustLevel,
              algortihmCurve = curveAlgorithm
            )

            // test
            TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

              // verify
              val expectedTrust = TrustedKeyResult(
                depth = 1,
                trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
                publicKey = twoKeyPairs.keyMaterialB.publicKey
              )
              result shouldBe Right(Set(expectedTrust))

            }
          }
        }
      }

      scenario("key A trusts B; minTrustLevel < trustLevel --> key B") {

        // prepare
        val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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
              minTrust = signedTrustRelationAToB.trustRelation.trustLevel - 1,
              algortihmCurve = curveAlgorithm
            )

            // test
            TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

              // verify
              val expectedTrust = TrustedKeyResult(
                depth = 1,
                trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
                publicKey = twoKeyPairs.keyMaterialB.publicKey
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
        val keyPairsAAndB = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
        val keyPairsCAndD = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
        val keyPairsEAndF = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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
              minTrust = signedTrustRelationAToB.trustRelation.trustLevel - 1,
              algortihmCurve = curveAlgorithm
            )

            // test
            TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

              // verify
              val expectedTrustedB = TrustedKeyResult(
                depth = 1,
                trustLevel = signedTrustRelationAToB.trustRelation.trustLevel,
                publicKey = keyB.publicKey
              )
              val expectedTrustedC = TrustedKeyResult(
                depth = 1,
                trustLevel = signedTrustRelationAToC.trustRelation.trustLevel,
                publicKey = keyC.publicKey
              )
              val expectedTrustedD = TrustedKeyResult(
                depth = 1,
                trustLevel = signedTrustRelationAToD.trustRelation.trustLevel,
                publicKey = keyD.publicKey
              )
              result shouldBe Right(Set(expectedTrustedB, expectedTrustedC, expectedTrustedD))

            }
          }
        }
      }

      scenario("web-of-trust with two users and each have a device; depth=3, minTrust=50 --> (user1, user2, deviceB)") {

        prepareWebOfTrust(curveAlgorithm) flatMap { webOfTrust =>

          val keyDeviceA = webOfTrust.deviceA

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = keyDeviceA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = keyDeviceA.privateKeyString,
            depth = 3,
            algortihmCurve = curveAlgorithm
          )

          // test
          TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

            // verify

            val expectedTrustedUser1 = TrustedKeyResult(
              depth = 1,
              trustLevel = 100,
              publicKey = webOfTrust.user1.publicKey
            )
            val expectedTrustedUser2 = TrustedKeyResult(
              depth = 2,
              trustLevel = 50,
              publicKey = webOfTrust.user2.publicKey
            )
            val expectedTrustedDeviceB = TrustedKeyResult(
              depth = 3,
              trustLevel = 100,
              publicKey = webOfTrust.deviceB.publicKey
            )
            result shouldBe Right(Set(expectedTrustedUser1, expectedTrustedUser2, expectedTrustedDeviceB))

          }
        }
      }

      scenario("web-of-trust with two users and each have a device; depth=3, minTrust=60 --> (user1)") {

        prepareWebOfTrust(curveAlgorithm) flatMap { webOfTrust =>

          val keyDeviceA = webOfTrust.deviceA

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = keyDeviceA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = keyDeviceA.privateKeyString,
            minTrust = 60,
            depth = 3,
            algortihmCurve = curveAlgorithm
          )

          // test
          TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

            // verify

            val expectedTrustedUser1 = TrustedKeyResult(
              depth = 1,
              trustLevel = 100,
              publicKey = webOfTrust.user1.publicKey
            )
            result shouldBe Right(Set(expectedTrustedUser1))

          }
        }
      }

      scenario("web-of-trust with two users and each have a device; depth=2, minTrust=50 --> (user1, user2)") {

        prepareWebOfTrust(curveAlgorithm) flatMap { webOfTrust =>

          val keyDeviceA = webOfTrust.deviceA

          val findTrustedSigned = TestDataGeneratorDb.findTrustedSigned(
            sourcePublicKey = keyDeviceA.publicKey.pubKeyInfo.pubKey,
            sourcePrivateKey = keyDeviceA.privateKeyString,
            depth = 2,
            algortihmCurve = curveAlgorithm
          )
          logger.debug(s"findTrustedSigned.json=${Json4sUtil.any2String(findTrustedSigned)}")

          // test
          TrustManager.findTrusted(findTrustedSigned, curveAlgorithm) map { result =>

            // verify

            val expectedTrustedUser1 = TrustedKeyResult(
              depth = 1,
              trustLevel = 100,
              publicKey = webOfTrust.user1.publicKey
            )
            val expectedTrustedUser2 = TrustedKeyResult(
              depth = 2,
              trustLevel = 50,
              publicKey = webOfTrust.user2.publicKey
            )
            result shouldBe Right(Set(expectedTrustedUser1, expectedTrustedUser2))

          }
        }
      }
    }


  feature("findTrusted() - ECDSA") {
    scenariosFor(findTrusted(ECDSA))
  }

  feature("findTrusted() - EDDSA") {
    scenariosFor(findTrusted(EDDSA))
  }

  private def testWithInvalidTrustLevel(trustLevel: Int, curveAlgorithm: String): Future[Assertion] = {

    // prepare
    val twoKeyPairs = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

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

    Future.sequence(trustSet map (TrustManager.upsert(_)))

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

  private def prepareWebOfTrust(curveAlgorithm: String): Future[WebOfTrustKeyPairs] = {

    /* user1 <--trustLevel=100--> deviceA
     * user1 <--trustLevel=50--> user2
     * user2 <--trustLevel=100--> deviceB
     */

    // prepare
    val keyPairsAAndB = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)
    val keyPairsCAndD = TestDataGeneratorDb.generateTwoKeyPairs(curveAlgorithm)

    val publicKeys = keyPairsAAndB.publicKeys ++ keyPairsCAndD.publicKeys
    persistPublicKeys(publicKeys) flatMap { publicKeysPersisted =>

      val expectedPublicKeysPersisted = publicKeys map (pk => Right(Some(pk)))
      publicKeysPersisted shouldBe expectedPublicKeysPersisted

      val keyUser1 = keyPairsAAndB.keyMaterialA
      val keyDeviceA = keyPairsAAndB.keyMaterialB
      val keyUser2 = keyPairsCAndD.keyMaterialA
      val keyDeviceB = keyPairsCAndD.keyMaterialB

      val signedTrustRelationUser1ToDeviceA = TestDataGeneratorDb.signedTrustRelation(keyUser1, keyDeviceA, 100)
      val signedTrustRelationDeviceAToUser1 = TestDataGeneratorDb.signedTrustRelation(keyDeviceA, keyUser1, 100)

      val signedTrustRelationUser1ToUser2 = TestDataGeneratorDb.signedTrustRelation(keyUser1, keyUser2)
      val signedTrustRelationUser2ToUser1 = TestDataGeneratorDb.signedTrustRelation(keyUser2, keyUser1)

      val signedTrustRelationUser2ToDeviceB = TestDataGeneratorDb.signedTrustRelation(keyUser2, keyDeviceB, 100)
      val signedTrustRelationDeviceBToUser2 = TestDataGeneratorDb.signedTrustRelation(keyDeviceB, keyUser2, 100)

      val signedTrustRelations = Set(
        signedTrustRelationUser1ToDeviceA,
        signedTrustRelationDeviceAToUser1,
        signedTrustRelationUser1ToUser2,
        signedTrustRelationUser2ToUser1,
        signedTrustRelationUser2ToDeviceB,
        signedTrustRelationDeviceBToUser2
      )
      persistTrustSet(signedTrustRelations) map { persistedTrust =>

        val expectedPersistedTrust = signedTrustRelations map (signedTrust => Right(signedTrust))
        persistedTrust shouldBe expectedPersistedTrust

        WebOfTrustKeyPairs(
          user1 = keyUser1,
          deviceA = keyDeviceA,
          user2 = keyUser2,
          deviceB = keyDeviceB
        )

      }

    }

  }

}

case class WebOfTrustKeyPairs(user1: KeyMaterialDb,
                              deviceA: KeyMaterialDb,
                              user2: KeyMaterialDb,
                              deviceB: KeyMaterialDb
                             )