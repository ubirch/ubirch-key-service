package com.ubirch.keyservice.client.rest.cache.redis

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.stream.Materializer
import com.ubirch.key.model.rest.{FindTrustedSigned, PublicKey, TrustedKeyResult}
import com.ubirch.keyservice.client.rest.KeyServiceClientRestBase
import com.ubirch.util.model.JsonErrorResponse
import com.ubirch.util.redis.RedisClientUtil
import org.json4s.native.Serialization.read

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * author: cvandrei
  * since: 2018-09-05
  */
object KeyServiceClientRestCacheRedis extends KeyServiceClientRestBase {

  def findPubKeyCached(publicKey: String)
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

  def currentlyValidPubKeysCached(hardwareId: String)
                                 (implicit httpClient: HttpExt, materializer: Materializer, system: ActorSystem): Future[Option[Set[PublicKey]]] = {

    logger.debug(s"currentlyValidPubKeysCached(): hardwareId=$hardwareId")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val redis = RedisClientUtil.getRedisClient
    val cacheKey = CacheHelperUtil.cacheKeyHardwareId(hardwareId)
    redis.get[String](cacheKey) flatMap {

      case None =>

        super.currentlyValidPubKeys(hardwareId) flatMap (KeyServiceClientRedisCacheUtil.cacheValidKeys(hardwareId, _))

      case Some(json) =>

        val pubKeySet = read[Set[PublicKey]](json)
        if (pubKeySet.isEmpty)
          super.currentlyValidPubKeys(hardwareId) flatMap (KeyServiceClientRedisCacheUtil.cacheValidKeys(hardwareId, _))
        else
          Future(Some(pubKeySet))

    }

  }

  def pubKeyTrustedGETCached(findTrustedSigned: FindTrustedSigned)
                            (implicit httpClient: HttpExt, materializer: Materializer, system: ActorSystem): Future[Either[JsonErrorResponse, Set[TrustedKeyResult]]] = {

    logger.debug(s"pubKeyTrustedGETCached(): findTrustedSigned=$findTrustedSigned")

    val sourcePubKey = findTrustedSigned.findTrusted.sourcePublicKey
    val depth = findTrustedSigned.findTrusted.depth
    val minTrust = findTrustedSigned.findTrusted.minTrustLevel

    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val redis = RedisClientUtil.getRedisClient
    val cacheKey = CacheHelperUtil.cacheKeyFindTrusted(sourcePubKey, depth, minTrust)
    redis.get[String](cacheKey) flatMap {

      case None =>

        super.pubKeyTrustedGET(findTrustedSigned) flatMap (KeyServiceClientRedisCacheUtil.cacheTrustedKeys(findTrustedSigned, _))

      case Some(json) =>

        Future(Right(read[Set[TrustedKeyResult]](json)))

    }

  }

}
