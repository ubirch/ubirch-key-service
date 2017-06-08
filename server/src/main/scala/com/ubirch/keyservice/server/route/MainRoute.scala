package com.ubirch.keyservice.server.route

import com.ubirch.keyservice.util.server.RouteConstants

import org.anormcypher.Neo4jConnection

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/**
  * author: cvandrei
  * since: 2017-03-22
  */
class MainRoute(implicit neo4jConnection: Neo4jConnection) {

  val welcome = new WelcomeRoute {}
  val deepCheck = new DeepCheckRoute {}
  val pubKey = new PublicKeyRoute {}

  val myRoute: Route = {

    pathPrefix(RouteConstants.apiPrefix) {
      pathPrefix(RouteConstants.serviceName) {
        pathPrefix(RouteConstants.currentVersion) {

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
