package com.ubirch.keyservice.server.route

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.pattern.CircuitBreaker
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.util.http.response.ResponseUtil

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.util.Try


trait WithCircuitBreaker {

  def system: ActorSystem

  def getCircuitBreaker(maxFailures: Int = 10, callTimeout: FiniteDuration = 5 seconds, resetTimeout: FiniteDuration = 6 seconds): CircuitBreaker = {
    CircuitBreaker(
      scheduler = system.scheduler,
      maxFailures = maxFailures,
      callTimeout = callTimeout,
      resetTimeout = resetTimeout
    )
  }

  lazy val defaultCircuitBreaker: CircuitBreaker = getCircuitBreaker()

}

trait WithRoutesHelpers extends WithCircuitBreaker with StrictLogging with FutureDirectives {

  ru: ResponseUtil =>

  class OnComplete[T <: Any](future: => Future[T])(implicit ec: ExecutionContext, classTag: ClassTag[T]) {

    def onCompleteWithNoCircuitBreaker:Directive1[Try[T]] = onComplete(future.recoverWith {
      case e: Exception =>
        logger.error("OOPs, (OC-CB) something happened: ", e)
        Future.failed(e)
    })

    def onCompleteWithCircuitBreaker(circuitBreaker: CircuitBreaker):Directive1[Try[T]] = {
      onCompleteWithBreaker(circuitBreaker)(future.recoverWith {
        case e: Exception =>
          logger.error("OOPs, (OC) something happened: ", e)
          Future.failed(e)
      })
    }

    def fold(maybeCircuitBreaker: => Option[CircuitBreaker] = Some(defaultCircuitBreaker)): Directive1[Try[T]] = {
      maybeCircuitBreaker.fold(onCompleteWithNoCircuitBreaker)(onCompleteWithCircuitBreaker)
    }

  }

  object OnComplete {
    def apply[T <: Any](future: => Future[T])(implicit ec: ExecutionContext, classTag: ClassTag[T]): OnComplete[T] =
      new OnComplete(future)
  }


}
