package com.ubirch.keyservice.server.route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.key.model._
import com.ubirch.key.model.rest.{PublicKey, PublicKeys}
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.server.actor.{ByPublicKey, CreatePublicKey, PublicKeyActor, QueryCurrentlyValid}
import com.ubirch.keyservice.util.server.RouteConstants
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonErrorResponse
import com.ubirch.util.rest.akka.directives.CORSDirective
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import org.anormcypher.Neo4jREST

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
      } ~ path(Segment) { pubKeyString =>
        respondWithCORS {

          get {
            findByPublicKey(pubKeyString)
          }

        }
      }
    }

  }

  private def createPublicKey(publicKey: PublicKey): Route = {

    onComplete(pubKeyActor ? CreatePublicKey(publicKey)) {

      case Failure(t) =>
        logger.error("create public key call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>

        resp match {

          case Some(createPubKey: PublicKey) =>
            complete(createPubKey)

          case jr: JsonErrorResponse =>
            logger.error(s"failed to create public key ${jr.errorMessage}")
            complete(StatusCodes.BadRequest -> jr)

          case None =>
            logger.error("failed to create public key (None)")
            complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "CreateError", errorMessage = "failed to create public key"))

          case _ =>
            logger.error("failed to create public key (server error)")
            complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "failed to create public key"))

        }

    }

  }

  private def queryCurrentlyValid(hardwareId: String): Route = {

    onComplete(pubKeyActor ? QueryCurrentlyValid(hardwareId)) {

      case Failure(t) =>
        logger.error("query currently valid public keys call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>

        resp match {

          case publicKeys: PublicKeys =>
            complete(publicKeys.publicKeys)

          case jr: JsonErrorResponse =>
            complete(StatusCodes.BadRequest -> jr)

          case _ =>
            logger.error("failed to create public key (server error)")
            complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "ServerError", errorMessage = "failed to create public key"))

        }

    }

  }

  private def findByPublicKey(publicKey: String): Route = {

    onComplete(pubKeyActor ? ByPublicKey(publicKey)) {

      case Failure(t) =>
        logger.error("find public key call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>

        resp match {

          case Some(createPubKey: db.PublicKey) => complete(Json4sUtil.any2any[rest.PublicKey](createPubKey))

          case None =>
            logger.error(s"failed to find public key ($publicKey)")
            complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "QueryError", errorMessage = "failed to find public key"))

          case _ =>
            logger.error("failed to find public key (server error)")
            complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "failed to find public key"))

        }

    }

  }

}
