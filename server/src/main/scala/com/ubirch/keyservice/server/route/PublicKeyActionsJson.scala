package com.ubirch.keyservice.server.route

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.{PublicKey, PublicKeyDelete, PublicKeys}
import com.ubirch.key.model.{db, rest}
import com.ubirch.keyservice.config.KeyConfig
import com.ubirch.keyservice.server.actor.{ByPublicKey, CreatePublicKey, QueryCurrentlyValid}
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonErrorResponse

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{FutureDirectives, RouteDirectives}
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Add description.
  *
  * @author Matthias L. Jugel
  */
trait PublicKeyActionsJson extends ResponseUtil {
  this: RouteDirectives with FutureDirectives with StrictLogging =>

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  protected val pubKeyActor: ActorRef

  implicit val timeout: Timeout = Timeout(KeyConfig.actorTimeout seconds)

  def createPublicKey(publicKey: PublicKey): Route = {

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

  def queryCurrentlyValid(hardwareId: String): Route = {

    onComplete(pubKeyActor ? QueryCurrentlyValid(hardwareId)) {

      case Failure(t) =>
        logger.error("query currently valid public keys call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>
        resp match {

          case publicKeys: PublicKeys =>
            complete(StatusCodes.OK -> publicKeys.publicKeys)

          case jr: JsonErrorResponse =>
            complete(StatusCodes.BadRequest -> jr)

          case _ =>
            logger.error("failed to create public key (server error)")
            complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "ServerError", errorMessage = "failed to create public key"))

        }

    }

  }

  def findByPublicKey(publicKey: String): Route = {

    onComplete(pubKeyActor ? ByPublicKey(publicKey)) {

      case Failure(t) =>
        logger.error("find public key call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>

        resp match {

          case Some(createPubKey: db.PublicKey) =>
            complete(Json4sUtil.any2any[rest.PublicKey](createPubKey))

          case None =>
            logger.error(s"failed to find public key ($publicKey)")
            complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "QueryError", errorMessage = "failed to find public key"))

          case _ =>
            logger.error("failed to find public key (server error)")
            complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "failed to find public key"))

        }

    }

  }

  def deletePublicKey(publicKeyDelete: PublicKeyDelete): Route = {

    onComplete(pubKeyActor ? publicKeyDelete) {

      case Failure(t) =>

        logger.error("delete public key call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(serverErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

      case Success(resp) =>

        resp match {

          case true => complete(StatusCodes.OK)

          case false =>
            logger.error(s"failed to delete public key ($publicKeyDelete)")
            complete(requestErrorResponse(errorType = "DeleteError", errorMessage = "failed to delete public key"))

        }

    }

  }
}
