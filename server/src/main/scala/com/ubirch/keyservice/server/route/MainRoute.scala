package com.ubirch.keyservice.server.route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.ubirch.keyservice.util.server.RouteConstants
import org.anormcypher.Neo4jREST

/**
  * author: cvandrei
  * since: 2017-03-22
  */
class MainRoute(implicit neo4jREST: Neo4jREST) {

  val welcome = new WelcomeRoute {}
  val deepCheck = new DeepCheckRoute {}
  val pubKey = new PublicKeyRoute {}
  val pubKeyMsgPack = new PublicKeyMsgPackRoute {}

  val myRoute: Route = {

    pathPrefix(RouteConstants.apiPrefix) {
      pathPrefix(RouteConstants.serviceName) {
        pathPrefix(RouteConstants.currentVersion) {
          pubKeyMsgPack.route ~
            pubKey.route ~
            deepCheck.route ~
            path(RouteConstants.check) {
              welcome.route
            } ~ pathEndOrSingleSlash {
            welcome.route
          }

        }
      }
    } ~
      pathSingleSlash {
        welcome.route
      }

  }

}
