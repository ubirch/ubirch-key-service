package com.ubirch.keyservice.client.rest.cache.redis

import com.ubirch.key.model.rest.PublicKey
import com.ubirch.keyservice.client.rest.KeyServiceClientRestBase
import com.ubirch.util.redis.RedisClientUtil

import org.json4s.native.Serialization.read

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.stream.Materializer

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * author: cvandrei
  * since: 2018-09-05
  */
object KeyServiceClientRestCacheRedis extends KeyServiceClientRestBase {

  def findPubKey(publicKey: String)
                (implicit httpClient: HttpExt, materializer: Materializer, system: ActorSystem): Future[Option[PublicKey]] = {

    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val redis = RedisClientUtil.getRedisClient
    val cacheKey = CacheHelperUtil.cacheKeyPublicKey(publicKey)
    redis.get[String](cacheKey) flatMap {

      case None =>

        super.findPubKey(publicKey) flatMap KeyServiceClientRedisCacheUtil.cachePublicKey

      case Some(json) =>

        Future(Some(read[PublicKey](json)))

    }

  }

  def currentlyValidPubKeys(hardwareId: String)
                           (implicit httpClient: HttpExt, materializer: Materializer, system: ActorSystem): Future[Option[Set[PublicKey]]] = {

    logger.debug(s"currentlyValidPubKeys(): hardwareId=$hardwareId")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val redis = RedisClientUtil.getRedisClient
    val cacheKey = CacheHelperUtil.cacheKeyHardwareId(hardwareId)
    redis.get[String](cacheKey) flatMap {

      case None =>

        super.currentlyValidPubKeys(hardwareId) flatMap (KeyServiceClientRedisCacheUtil.cacheValidKeys(hardwareId, _))

      case Some(json) =>

        Future(Some(read[Set[PublicKey]](json)))

    }

  }

}
