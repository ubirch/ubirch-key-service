package com.ubirch.keyservice.server.route

import com.ubirch.keyservice.util.server.RouteConstants

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/**
  * author: cvandrei
  * since: 2017-03-22
  */
class MainRoute {

  val welcome = new WelcomeRoute {}

  val myRoute: Route = {

    pathPrefix(RouteConstants.apiPrefix) {
      pathPrefix(RouteConstants.serviceName) {
        pathPrefix(RouteConstants.currentVersion) {

          pathEndOrSingleSlash {
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
