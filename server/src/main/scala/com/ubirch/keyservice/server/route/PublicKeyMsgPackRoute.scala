package com.ubirch.keyservice.server.route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.server.actor.PublicKeyActor
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.util.server.RouteConstants
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.model.JsonResponse
import com.ubirch.util.rest.akka.directives.CORSDirective
import org.anormcypher.Neo4jREST

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyMsgPackRoute(implicit neo4jREST: Neo4jREST)
  extends ResponseUtil
    with CORSDirective
    with StrictLogging {

  implicit val system = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout = Timeout(Config.actorTimeout seconds)

  private val pubKeyActor = system.actorOf(PublicKeyActor.props(), ActorNames.PUB_KEY)

  val route: Route = {

    pathPrefix(RouteConstants.pubKey / RouteConstants.mpack) {
      pathEnd {
        post {
          entity(as[Array[Byte]]) { binData =>
            complete(StatusCodes.Accepted -> JsonResponse(message = "pubKey created").toJsonString)
          }
        }
      }
    }
  }
}
