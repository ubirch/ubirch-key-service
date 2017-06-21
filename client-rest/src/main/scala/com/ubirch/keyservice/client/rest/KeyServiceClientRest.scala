package com.ubirch.keyservice.client.rest

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo}
import com.ubirch.keyservice.client.rest.config.KeyClientRestConfig
import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.json.MyJsonProtocol
import com.ubirch.util.model.JsonResponse

import org.joda.time.DateTime
import org.json4s.native.Serialization.read

import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-06-20
  */
object KeyServiceClientRest extends MyJsonProtocol
  with StrictLogging
  with DefaultReads {

  private val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z"
  implicit private val dateFormat = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit private val jsonResponseFormat = Json.format[JsonResponse]
  implicit private val deepCheckFormat = Json.format[DeepCheckResponse]
  implicit private val publicKeyInfoFormat = Json.format[PublicKeyInfo]
  implicit private val publicKeyFormat = Json.format[PublicKey]

  def check()(implicit ws: WSClient): Future[Option[JsonResponse]] = {

    val url = KeyClientRestConfig.urlCheck
    try {

      ws.url(url).get() map { res =>

        if (200 == res.status) {
          logger.debug(s"$url: got check: ${res.body}")
          res.json.asOpt[JsonResponse]
        } else {
          logErrorAndReturnNone(s"$url: status=${res.status}, body=${res.body}")
        }

      }

    } catch {
      case e: Exception => Future(logErrorAndReturnNone(s"$url failed with an Exception", Some(e)))
      case re: RuntimeException => Future(logErrorAndReturnNone(s"$url failed with a RuntimeException", Some(re)))
    }

  }

  def deepCheck()(implicit ws: WSClient): Future[Option[DeepCheckResponse]] = {

    val url = KeyClientRestConfig.urlDeepCheck
    try {

      ws.url(url).get() map { res =>

        if (Set(200, 503).contains(res.status)) {
          logger.debug(s"$url: got: ${res.body}")
          res.json.asOpt[DeepCheckResponse]
        } else {
          logErrorAndReturnNone(s"$url: status=${res.status}, body=${res.body}")
        }

      }

    } catch {
      case e: Exception => Future(logErrorAndReturnNone(s"$url failed with an Exception", Some(e)))
      case re: RuntimeException => Future(logErrorAndReturnNone(s"$url failed with a RuntimeException", Some(re)))
    }

  }

  def pubKey(publicKey: PublicKey)
            (implicit ws: WSClient): Future[Option[PublicKey]] = {

    val url = KeyClientRestConfig.pubKey
    try {

      val json = publicKeyFormat.writes(publicKey)
      logger.debug(s"pubKey (object): $publicKey")
      logger.debug(s"pubKey (json): ${Json.prettyPrint(json)}")
      ws.url(url).post(json) map { res =>

        if (200 == res.status) {
          logger.debug(s"$url: got: ${res.body}")
          Some(read[PublicKey](res.json.toString()))
        } else {
          logErrorAndReturnNone(s"$url: status=${res.status}, body=${res.body}")
        }

      }

    } catch {
      case e: Exception => Future(logErrorAndReturnNone(s"$url failed with an Exception", Some(e)))
      case re: RuntimeException => Future(logErrorAndReturnNone(s"$url failed with a RuntimeException", Some(re)))
    }

  }

  def currentlyValidPubKeys(hardwareId: String)
                           (implicit ws: WSClient): Future[Option[Set[PublicKey]]] = {

    val url = KeyClientRestConfig.currentlyValidPubKeys(hardwareId)
    try {

      ws.url(url).get() map { res =>

        if (200 == res.status) {
          logger.debug(s"$url: got: ${res.body}")
          Some(read[Set[PublicKey]](res.json.toString()))
        } else {
          logErrorAndReturnNone(s"$url: status=${res.status}, body=${res.body}")
        }

      }

    } catch {
      case e: Exception => Future(logErrorAndReturnNone(s"$url failed with an Exception", Some(e)))
      case re: RuntimeException => Future(logErrorAndReturnNone(s"$url failed with a RuntimeException", Some(re)))
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
