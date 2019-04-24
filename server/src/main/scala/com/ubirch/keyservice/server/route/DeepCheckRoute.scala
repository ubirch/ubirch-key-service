package com.ubirch.keyservice.server.route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.pattern.{CircuitBreaker, ask}
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.keyservice.config.KeySvcConfig
import com.ubirch.keyservice.server.actor.DeepCheckActor
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.util.server.RouteConstants
import com.ubirch.util.deepCheck.model.{DeepCheckRequest, DeepCheckResponse}
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.rest.akka.directives.CORSDirective
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import org.neo4j.driver.v1.Driver

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2017-06-08
  */
class DeepCheckRoute(implicit neo4jDriver: Driver)
  extends CORSDirective
    with ResponseUtil
    with WithRoutesHelpers
    with StrictLogging {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(KeySvcConfig.actorTimeout seconds)

  private val deepCheckActor = system.actorOf(DeepCheckActor.props(), ActorNames.DEEP_CHECK)

  override lazy val defaultCircuitBreaker: CircuitBreaker = getCircuitBreaker().onCallTimeout(_ => logger.error("DeepCheckRequest request timed out -route-"))

  val route: Route = {

    path(RouteConstants.deepCheck) {
      respondWithCORS {
        get {

          OnComplete(deepCheckActor ? DeepCheckRequest()).fold() {

            case Failure(t) =>
              logger.error("failed to run deepCheck (1)", t)
              complete(serverErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

            case Success(resp) =>
              resp match {

                case res: DeepCheckResponse if res.status => complete(res)
                case res: DeepCheckResponse if !res.status => complete(response(responseObject = res, status = StatusCodes.ServiceUnavailable))
                case _ =>
                  logger.error("failed to run deepCheck (2)")
                  complete(serverErrorResponse(errorType = "ServerError", errorMessage = "failed to run deep check"))

              }

          }

        }
      }
    }

  }

}
