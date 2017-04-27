package com.ubirch.keyservice.core.manager

import com.ubirch.key.model.rest.PublicKey

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-04-27
  */
object PublicKeyManager {

  def create(toCreate: PublicKey): Future[Option[PublicKey]] = {

    // TODO automated tests
    // TODO store in Neo4j
    Future(Some(toCreate))

  }

}
