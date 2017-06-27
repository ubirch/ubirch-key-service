package com.ubirch.keyservice.server.route

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.{PublicKey, PublicKeys}
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.server.actor.{CreatePublicKey, PublicKeyActor, QueryCurrentlyValid}
import com.ubirch.keyservice.util.server.RouteConstants
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.rest.akka.directives.CORSDirective

import org.anormcypher.Neo4jREST

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyRoute(implicit neo4jREST: Neo4jREST)
  extends ResponseUtil
    with CORSDirective
    with StrictLogging {

  implicit val system = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout = Timeout(Config.actorTimeout seconds)

  private val pubKeyActor = system.actorOf(PublicKeyActor.props(), ActorNames.PUB_KEY)

  val route: Route = {

    pathPrefix(RouteConstants.pubKey) {
      pathEnd {
        respondWithCORS {

          post {
            entity(as[PublicKey]) { publicKey =>
              createPublicKey(publicKey)
            }
          }

        }

      } ~ path(RouteConstants.current / RouteConstants.hardwareId / Segment) { hardwareId =>
        respondWithCORS {

          get {
            queryCurrentlyValid(hardwareId)
          }

        }
      }
    }

  }

  private def createPublicKey(publicKey: PublicKey): Route = {

    onComplete(pubKeyActor ? CreatePublicKey(publicKey)) {

      case Failure(t) =>
        logger.error("create public key call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(serverErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>

        resp match {

          case Some(createPubKey: PublicKey) => complete(createPubKey)

          case None =>
            logger.error("failed to create public key (None)")
            complete(requestErrorResponse(errorType = "CreateError", errorMessage = "failed to create public key"))

          case _ =>
            logger.error("failed to create public key (server error)")
            complete(serverErrorResponse(errorType = "ServerError", errorMessage = "failed to create public key"))

        }

    }

  }

  private def queryCurrentlyValid(hardwareId: String): Route = {

    onComplete(pubKeyActor ? QueryCurrentlyValid(hardwareId)) {

      case Failure(t) =>
        logger.error("query currently valid public keys call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(serverErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>

        resp match {

          case publicKeys: PublicKeys => complete(publicKeys.publicKeys)

          case _ =>
            logger.error("failed to create public key (server error)")
            complete(serverErrorResponse(errorType = "ServerError", errorMessage = "failed to create public key"))

        }

    }

  }

}
