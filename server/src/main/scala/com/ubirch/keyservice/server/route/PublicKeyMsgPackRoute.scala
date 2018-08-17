package com.ubirch.keyservice.server.route

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.core.protocol.msgpack.UbMsgPacker
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo}
import com.ubirch.keyservice.server.actor.PublicKeyActor
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.util.server.RouteConstants.{mpack, pubKey}
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.model.JsonErrorResponse
import com.ubirch.util.rest.akka.directives.CORSDirective

import org.apache.commons.codec.binary.Hex
import org.neo4j.driver.v1.Driver

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyMsgPackRoute(implicit neo4jDriver: Driver)
  extends ResponseUtil
    with CORSDirective
    with StrictLogging
    with PublicKeyActionsString {

  override protected val pubKeyActor: ActorRef = system.actorOf(PublicKeyActor.props(), ActorNames.PUB_KEY)

  val route: Route = pathPrefix(pubKey / mpack) {
    pathEnd {
      respondWithCORS {
        post {
          entity(as[Array[Byte]]) { binData =>
            val hexData = Hex.encodeHexString(binData)
            logger.debug(s"got msgPack: $hexData")

            UbMsgPacker.processUbirchprot(binData).map { ubm =>
              val u = ubm.payloads.data

              ubm.payloads.data.extractOpt[PublicKeyInfo] match {
                case Some(pki) =>
                  Some(PublicKey(
                    pubKeyInfo = pki,
                    signature = ubm.signature.getOrElse(""),
                    previousPubKeySignature = ubm.prevSignature,
                    raw = Some(ubm.rawMessage)
                  ))
                case None =>
                  None
              }
            }.headOption match {
              case Some(publicKey) if publicKey.isDefined =>
                createPublicKey(publicKey.get)
              case Some(publicKey) if publicKey.isEmpty => // TODO question: this is the same as `case None =>`, isn't it?
                logger.error("failed to parse input")
                import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
                complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "ValidationError", errorMessage = "request does not contain a key"))
              case None =>
                logger.error("failed to create public key (server error)")
                import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
                complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "ServerError", errorMessage = "request does not contain a key"))
            }
          }
        }
      }
    }
  }
}
