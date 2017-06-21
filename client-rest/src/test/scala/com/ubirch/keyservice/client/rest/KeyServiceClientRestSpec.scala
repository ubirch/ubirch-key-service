package com.ubirch.keyservice.client.rest

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model._
import com.ubirch.key.model.db.PublicKey
import com.ubirch.keyService.testTools.data.generator.TestDataGeneratorDb
import com.ubirch.keyService.testTools.db.neo4j.Neo4jSpec
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.core.manager.PublicKeyManager
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonResponse

import org.joda.time.DateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ning.NingWSClient

/**
  * author: cvandrei
  * since: 2017-06-20
  */
class KeyServiceClientRestSpec extends Neo4jSpec {

  implicit val system = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  implicit val materializer = ActorMaterializer()

  implicit val ws = NingWSClient()

  override def afterAll(): Unit = {
    super.afterAll()
    ws.close()
    system.terminate()
  }

  feature("check()") {

    scenario("check without errors") {

      // test
      KeyServiceClientRest.check() map {

        // verify
        case None => fail("expected a result other than None")

        case Some(jsonResponse: JsonResponse) =>
          val goInfo = s"${Config.goPipelineName} / ${Config.goPipelineLabel} / ${Config.goPipelineRevision}"
          val expected = JsonResponse(message = s"Welcome to the ubirchKeyService ( $goInfo )")
          jsonResponse should be(expected)

      }

    }

  }

  feature("deepCheck()") {

    scenario("check without errors") {

      // test
      KeyServiceClientRest.deepCheck() map {

        // verify
        case None => fail("expected a result other than None")
        case Some(deepCheckResponse: DeepCheckResponse) => deepCheckResponse should be(DeepCheckResponse())

      }

    }

  }

  feature("pubKey()") {

    scenario("new key") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1))
      )
      val restPubKey = Json4sUtil.any2any[rest.PublicKey](publicKey)

      // test
      KeyServiceClientRest.pubKey(restPubKey) map { result =>

        // verify
        result should be('isDefined)
        result.get should be(restPubKey)

      }

    }

    scenario("key already exists") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1),
        infoValidNotAfter = Some(DateTime.now.plusDays(1))
      )
      val restPubKey = Json4sUtil.any2any[rest.PublicKey](publicKey)
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail("failed to prepare public key")

        case Some(_: PublicKey) =>

          // test
          KeyServiceClientRest.pubKey(restPubKey) map { result =>

            // verify
            result should be('isEmpty)

          }

      }

    }

  }

  feature("currentlyValidPubKeys()") {

    scenario("has no keys") {

      // test
      KeyServiceClientRest.currentlyValidPubKeys("1234") map { result =>

        // verify
        result should be('isDefined)
        result.get should be('isEmpty)

      }

    }

    scenario("has valid key(s)") {

      // prepare
      val (pubKey1, privKey1) = EccUtil.generateEccKeyPairEncoded
      val publicKey = TestDataGeneratorDb.createPublicKey(
        privateKey = privKey1,
        infoPubKey = pubKey1,
        infoValidNotBefore = DateTime.now.minusDays(1)
      )
      PublicKeyManager.create(publicKey) flatMap {

        case None => fail("failed to prepare public key")

        case Some(existingPubKey: PublicKey) =>

          // test
          KeyServiceClientRest.currentlyValidPubKeys(existingPubKey.pubKeyInfo.hwDeviceId) map { result =>

            // verify
            result should be('isDefined)
            val actual = result.get map Json4sUtil.any2any[PublicKey]
            val expected = Set(Json4sUtil.any2any[PublicKey](existingPubKey))
            actual should be(expected)

          }

      }

    }

  }

}
