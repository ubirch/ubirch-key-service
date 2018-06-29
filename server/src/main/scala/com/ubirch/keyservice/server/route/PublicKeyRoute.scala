package com.ubirch.keyservice.server.route

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{FutureDirectives, RouteDirectives}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.key.model.rest.{PublicKey, PublicKeyDelete}
import com.ubirch.keyservice.config.Config
import com.ubirch.keyservice.server.actor.{CreatePublicKey, PublicKeyActor}
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.util.server.RouteConstants
import com.ubirch.util.http.response.ResponseUtil
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
    with RouteDirectives
    with FutureDirectives
    with StrictLogging
    with PublicKeyActions {

  override protected val pubKeyActor: ActorRef = system.actorOf(PublicKeyActor.props(), ActorNames.PUB_KEY)

  val route: Route = {

    pathPrefix(RouteConstants.pubKey) {
      pathEnd {
        respondWithCORS {

          post {
            entity(as[PublicKey]) { publicKey =>
              createPublicKey(publicKey)
            }
          } ~ delete {
            entity(as[PublicKeyDelete]) { publicKey =>
              deletePublicKey(publicKey)
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

}
