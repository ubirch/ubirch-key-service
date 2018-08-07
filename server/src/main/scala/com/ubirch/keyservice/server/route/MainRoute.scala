package com.ubirch.keyservice.server.route

import com.ubirch.keyservice.util.server.RouteConstants

import org.neo4j.driver.v1.Driver

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/**
  * author: cvandrei
  * since: 2017-03-22
  */
class MainRoute(implicit neo4jDriver: Driver) {

  private val welcome = new WelcomeRoute {}
  private val deepCheck = new DeepCheckRoute {}
  private val pubKey = new PublicKeyRoute {}
  private val pubKeyMsgPack = new PublicKeyMsgPackRoute {}

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
