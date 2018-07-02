package com.ubirch.keyservice.server.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.core.protocol.msgpack.UbMsgPacker
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo}
import com.ubirch.keyservice.server.actor.PublicKeyActor
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.util.server.RouteConstants.{mpack, pubKey}
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.model.JsonErrorResponse
import com.ubirch.util.rest.akka.directives.CORSDirective
import org.anormcypher.Neo4jREST
import org.apache.commons.codec.binary.Hex

import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyMsgPackRoute(implicit neo4jREST: Neo4jREST)
  extends ResponseUtil
    with CORSDirective
    with StrictLogging
    with PublicKeyActions {

  override protected val pubKeyActor: ActorRef = system.actorOf(PublicKeyActor.props(), ActorNames.PUB_KEY)

  val route: Route = pathPrefix(pubKey / mpack) {
    pathEnd {
      respondWithCORS {
        post {
          entity(as[Array[Byte]]) { binData =>
            val hexData = Hex.encodeHexString(binData)
            logger.debug(s"got msgPack: $hexData")

            UbMsgPacker.processUbirchprot(binData).map { ubm =>
              PublicKey(
                pubKeyInfo = ubm.payloads.data.extract[PublicKeyInfo],
                signature = ubm.signature.getOrElse(""),
                previousPubKeySignature = ubm.prevSignature,
                raw = Some(ubm.rawMessage)
              )
            }.headOption match {
              case Some(publicKey) =>
                createPublicKey(publicKey)
              case None =>
                logger.error("failed to create public key (server error)")
                complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "ServerError", errorMessage = "request does not contain a key").toJsonString)
            }
          }
        }
      }
    }
  }
}
