package com.ubirch.keyservice.server.route

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.{PublicKey, PublicKeyDelete, SignedTrustRelation}
import com.ubirch.keyservice.server.actor.{PublicKeyActor, TrustActor}
import com.ubirch.keyservice.server.actor.util.ActorNames
import com.ubirch.keyservice.util.server.RouteConstants
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.rest.akka.directives.CORSDirective

import org.neo4j.driver.v1.Driver

import akka.actor.ActorRef
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{FutureDirectives, RouteDirectives}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-04-27
  */
class PublicKeyRoute(implicit neo4jDriver: Driver)
  extends ResponseUtil
    with CORSDirective
    with RouteDirectives
    with FutureDirectives
    with StrictLogging
    with PublicKeyActionsJson {

  override protected val pubKeyActor: ActorRef = system.actorOf(PublicKeyActor.props(), ActorNames.PUB_KEY)
  override protected val trustActor: ActorRef = system.actorOf(TrustActor.props(), ActorNames.TRUST)

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

      } ~ path(RouteConstants.trust) {
        respondWithCORS {

          post {
            entity(as[SignedTrustRelation]) { trustedKey =>
              trustKey(trustedKey)
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
