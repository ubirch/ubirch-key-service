package com.ubirch.keyservice.client.rest

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.{FindTrustedSigned, PublicKey, PublicKeyDelete, SignedTrustRelation, TrustedKeyResult}
import com.ubirch.keyservice.client.rest.config.KeyClientRestConfig
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.deepCheck.util.DeepCheckResponseUtil
import com.ubirch.util.json.{Json4sUtil, MyJsonProtocol}
import com.ubirch.util.model.{JsonErrorResponse, JsonResponse}

import org.json4s.native.Serialization.read

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode, StatusCodes}
import akka.stream.Materializer
import akka.util.ByteString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-06-20
  */
object KeyServiceClientRest extends KeyServiceClientRestBase {}

trait KeyServiceClientRestBase extends MyJsonProtocol
  with StrictLogging {

  def check()(implicit httpClient: HttpExt, materializer: Materializer): Future[Option[JsonResponse]] = {


    val url = KeyClientRestConfig.urlCheck
    httpClient.singleRequest(HttpRequest(uri = url)) flatMap {

      case HttpResponse(StatusCodes.OK, _, entity, _) =>

        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
          Some(read[JsonResponse](body.utf8String))
        }

      case res@HttpResponse(code, _, _, _) =>

        res.discardEntityBytes()
        Future(
          logErrorAndReturnNone(s"check() call to key-service failed: url=$url code=$code, status=${res.status}")
        )

    }

  }

  def deepCheck()(implicit httpClient: HttpExt, materializer: Materializer): Future[DeepCheckResponse] = {

    val statusCodes: Set[StatusCode] = Set(StatusCodes.OK, StatusCodes.ServiceUnavailable)

    val url = KeyClientRestConfig.urlDeepCheck
    httpClient.singleRequest(HttpRequest(uri = url)) flatMap {

      case HttpResponse(status, _, entity, _) if statusCodes.contains(status) =>

        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
          read[DeepCheckResponse](body.utf8String)
        }

      case res@HttpResponse(code, _, _, _) =>

        res.discardEntityBytes()
        val errorText = s"deepCheck() call to key-service failed: url=$url code=$code, status=${res.status}"
        logger.error(errorText)
        val deepCheckRes = DeepCheckResponse(status = false, messages = Seq(errorText))
        Future(
          DeepCheckResponseUtil.addServicePrefix("key-service", deepCheckRes)
        )

    }

  }

  def pubKeyPOST(publicKey: PublicKey)
                (implicit httpClient: HttpExt, materializer: Materializer): Future[Option[PublicKey]] = {

    Json4sUtil.any2String(publicKey) match {

      case Some(pubKeyJsonString: String) =>

        logger.debug(s"pubKey (object): $pubKeyJsonString")
        val url = KeyClientRestConfig.pubKey
        val req = HttpRequest(
          method = HttpMethods.POST,
          uri = url,
          entity = HttpEntity.Strict(ContentTypes.`application/json`, data = ByteString(pubKeyJsonString))
        )
        httpClient.singleRequest(req) flatMap {

          case HttpResponse(StatusCodes.OK, _, entity, _) =>

            entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
              Some(read[PublicKey](body.utf8String))
            }

          case res@HttpResponse(code, _, _, _) =>

            res.discardEntityBytes()
            Future(
              logErrorAndReturnNone(s"pubKey() call to key-service failed: url=$url code=$code, status=${res.status}")
            )

        }

      case None =>
        logger.error(s"failed to to convert input to JSON: publicKey=$publicKey")
        Future(None)

    }

  }

  def pubKeyDELETE(publicKeyDelete: PublicKeyDelete)
                  (implicit httpClient: HttpExt, materializer: Materializer): Future[Boolean] = {

    Json4sUtil.any2String(publicKeyDelete) match {

      case Some(pubKeyDeleteString: String) =>

        logger.debug(s"pubKeyDelete (object): $pubKeyDeleteString")
        val url = KeyClientRestConfig.pubKey
        val req = HttpRequest(
          method = HttpMethods.DELETE,
          uri = url,
          entity = HttpEntity.Strict(ContentTypes.`application/json`, data = ByteString(pubKeyDeleteString))
        )
        httpClient.singleRequest(req) flatMap {

          case res@HttpResponse(StatusCodes.OK, _, entity, _) =>

            res.discardEntityBytes()
            Future(true)

          case res@HttpResponse(code, _, _, _) =>

            res.discardEntityBytes()
            logErrorAndReturnNone(s"pubKeyDELETE() call to key-service failed: url=$url code=$code, status=${res.status}")
            Future(false)

        }

      case None =>
        logger.error(s"failed to to convert input to JSON: publicKeyDelete=$publicKeyDelete")
        Future(false)

    }

  }

  def findPubKey(publicKey: String)
                (implicit httpClient: HttpExt, materializer: Materializer): Future[Option[PublicKey]] = {

    logger.debug(s"publicKey: $publicKey")
    val url = KeyClientRestConfig.findPubKey(publicKey)
    val req = HttpRequest(
      method = HttpMethods.GET,
      uri = url
    )
    httpClient.singleRequest(req) flatMap {

      case HttpResponse(StatusCodes.OK, _, entity, _) =>

        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
          Some(read[PublicKey](body.utf8String))
        }

      case res@HttpResponse(code, _, _, _) =>

        res.discardEntityBytes()
        Future(
          logErrorAndReturnNone(s"findPubKey() call to key-service failed: url=$url code=$code, status=${res.status}")
        )

    }

  }

  def pubKeyTrustPOST(signedTrustRelation: SignedTrustRelation)
                     (implicit httpClient: HttpExt, materializer: Materializer): Future[Either[JsonErrorResponse, SignedTrustRelation]] = {

    Json4sUtil.any2String(signedTrustRelation) match {

      case Some(trustJsonString: String) =>

        logger.debug(s"trust public key (JSON): $signedTrustRelation")
        val url = KeyClientRestConfig.pubKeyTrust
        val req = HttpRequest(
          method = HttpMethods.POST,
          uri = url,
          entity = HttpEntity.Strict(ContentTypes.`application/json`, data = ByteString(trustJsonString))
        )
        httpClient.singleRequest(req) flatMap {

          case HttpResponse(StatusCodes.OK, _, entity, _) =>

            entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
              Right(read[SignedTrustRelation](body.utf8String))
            }

          case res@HttpResponse(code, _, entity, _) =>

            res.discardEntityBytes()
            logger.error(s"pubKeyTrustPOST() call to key-service failed: url=$url code=$code, status=${res.status}")
            entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
              Left(read[JsonErrorResponse](body.utf8String))
            }

        }

      case None =>

        logger.error(s"failed to to convert input to JSON: signedTrustRelation=$signedTrustRelation")
        Future(Left(JsonErrorResponse(errorType = "RestClientError", errorMessage = "error before sending the request: failed to convert input to JSON")))

    }

  }

  def pubKeyTrustedGET(findTrustedSigned: FindTrustedSigned)
                     (implicit httpClient: HttpExt, materializer: Materializer): Future[Either[JsonErrorResponse, Set[TrustedKeyResult]]] = {

    // TODO UP-174 automated tests
    Json4sUtil.any2String(findTrustedSigned) match {

      case Some(trustJsonString: String) =>

        logger.debug(s"find trusted public keys (JSON): $findTrustedSigned")
        val url = KeyClientRestConfig.pubKeyTrusted
        val req = HttpRequest(
          method = HttpMethods.GET,
          uri = url,
          entity = HttpEntity.Strict(ContentTypes.`application/json`, data = ByteString(trustJsonString))
        )
        httpClient.singleRequest(req) flatMap {

          case HttpResponse(StatusCodes.OK, _, entity, _) =>

            entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
              Right(read[Set[TrustedKeyResult]](body.utf8String))
            }

          case res@HttpResponse(code, _, entity, _) =>

            res.discardEntityBytes()
            logger.error(s"pubKeyTrustedGET() call to key-service failed: url=$url code=$code, status=${res.status}")
            entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
              Left(read[JsonErrorResponse](body.utf8String))
            }

        }

      case None =>

        logger.error(s"failed to to convert input to JSON: findTrustedSigned=$findTrustedSigned")
        Future(Left(JsonErrorResponse(errorType = "RestClientError", errorMessage = "error before sending the request: failed to convert input to JSON")))

    }

  }

  def currentlyValidPubKeys(hardwareId: String)
                           (implicit httpClient: HttpExt, materializer: Materializer): Future[Option[Set[PublicKey]]] = {

    val url = KeyClientRestConfig.currentlyValidPubKeys(hardwareId)
    httpClient.singleRequest(HttpRequest(uri = url)) flatMap {

      case HttpResponse(StatusCodes.OK, _, entity, _) =>

        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
          Some(read[Set[PublicKey]](body.utf8String))
        }

      case res@HttpResponse(code, _, _, _) =>

        res.discardEntityBytes()
        Future(
          logErrorAndReturnNone(s"currentlyValidPubKeys() call to key-service failed: url=$url, code=$code, status=${res.status}")
        )

    }

  }

  private def logErrorAndReturnNone[T](errorMsg: String,
                                       t: Option[Throwable] = None
                                      ): Option[T] = {
    t match {
      case None => logger.error(errorMsg)
      case Some(someThrowable: Throwable) => logger.error(errorMsg, someThrowable)
    }

    None

  }

}
