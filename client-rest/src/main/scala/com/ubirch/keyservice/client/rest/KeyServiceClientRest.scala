package com.ubirch.keyservice.client.rest

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.PublicKey
import com.ubirch.keyservice.client.rest.config.KeyClientRestConfig
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.{Json4sUtil, MyJsonProtocol}
import com.ubirch.util.model.JsonResponse

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
object KeyServiceClientRest extends MyJsonProtocol
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

  def deepCheck()(implicit httpClient: HttpExt, materializer: Materializer): Future[Option[DeepCheckResponse]] = {

    val statusCodes: Set[StatusCode] = Set(StatusCodes.OK, StatusCodes.ServiceUnavailable)

    val url = KeyClientRestConfig.urlDeepCheck
    httpClient.singleRequest(HttpRequest(uri = url)) flatMap {

      case HttpResponse(status, _, entity, _) if statusCodes.contains(status) =>

        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map { body =>
          Some(read[DeepCheckResponse](body.utf8String))
        }

      case res@HttpResponse(code, _, _, _) =>

        res.discardEntityBytes()
        Future(
          logErrorAndReturnNone(s"deepCheck() call to key-service failed: url=$url code=$code, status=${res.status}")
        )

    }

  }

  def pubKey(publicKey: PublicKey)
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
