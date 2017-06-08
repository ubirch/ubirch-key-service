package com.ubirch.keyservice.server.route

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.server.actor.{DeepCheckActor, DeepCheckRequest}
import com.ubirch.keyservice.util.server.RouteConstants
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.model.DeepCheckResponse
import com.ubirch.util.rest.akka.directives.CORSDirective

import org.anormcypher.Neo4jConnection

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2017-06-08
  */
class DeepCheckRoute(implicit neo4jConnection: Neo4jConnection)
  extends CORSDirective
    with ResponseUtil
    with StrictLogging {

  implicit val system = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout = Timeout(Config.actorTimeout seconds)

  private val deepCheckActor = system.actorOf(new RoundRobinPool(Config.akkaNumberOfWorkers).props(Props(new DeepCheckActor())), ActorNames.DEEP_CHECK)

  val route: Route = {

    path(RouteConstants.deepCheck) {
      respondWithCORS {
        get {

          onComplete(deepCheckActor ? DeepCheckRequest()) {

            case Failure(t) =>
              logger.error("failed to run deepCheck (check DeepCheckRoute for bugs!!!)", t)
              complete(serverErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

            case Success(resp) =>
              resp match {

                case res: DeepCheckResponse =>
                  if (res.status == "OK") {
                    complete(res)
                  } else {
                    complete(serverErrorResponse(responseObject = res))
                  }

                case _ => complete(serverErrorResponse(errorType = "CreateError", errorMessage = "failed to create context"))

              }

          }

        }
      }
    }

  }

}
