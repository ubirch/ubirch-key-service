package com.ubirch.keyservice.server.route

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.PublicKey
import com.ubirch.keyservice.config.KeyConfig
import com.ubirch.keyservice.server.actor.CreatePublicKey
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.model.JsonErrorResponse

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{FutureDirectives, RouteDirectives}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Add description.
  *
  * @author Matthias L. Jugel
  */
trait PublicKeyActionsString {
  this: ResponseUtil with RouteDirectives with FutureDirectives with StrictLogging =>

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  protected val pubKeyActor: ActorRef

  implicit val timeout: Timeout = Timeout(KeyConfig.actorTimeout seconds)

  def createPublicKey(publicKey: PublicKey): Route = {

    onComplete(pubKeyActor ? CreatePublicKey(publicKey)) {

      case Failure(t) =>
        logger.error("create public key call responded with an unhandled message (check PublicKeyRoute for bugs!!!)", t)
        complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end").toJsonString)

      case Success(resp) =>

        resp match {

          case Some(createPubKey: PublicKey) =>
            complete(createPubKey.toString) // TODO: in the JSON version this is serialized to JSON. here it the scala object is serialized into a String. Question: is this the intention?

          case jr: JsonErrorResponse =>
            logger.error(s"failed to create public key ${jr.errorMessage}")
            complete(StatusCodes.BadRequest -> jr.toJsonString)

          case None =>
            logger.error("failed to create public key (None)")
            complete(StatusCodes.BadRequest -> JsonErrorResponse(errorType = "CreateError", errorMessage = "failed to create public key").toJsonString)

          case _ =>
            logger.error("failed to create public key (server error)")
            complete(StatusCodes.InternalServerError -> JsonErrorResponse(errorType = "ServerError", errorMessage = "failed to create public key").toJsonString)

        }

    }

  }

}
