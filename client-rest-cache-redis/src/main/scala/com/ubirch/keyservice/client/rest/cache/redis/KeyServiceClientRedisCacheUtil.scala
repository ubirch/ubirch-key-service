package com.ubirch.keyservice.client.rest.cache.redis

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.key.model.rest.{FindTrustedSigned, PublicKey, PublicKeyInfo, TrustedKeyResult}
import com.ubirch.keyservice.client.rest.cache.redis.config.KeyClientRedisConfig
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.model.JsonErrorResponse
import com.ubirch.util.redis.RedisClientUtil

import akka.actor.ActorSystem

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * author: cvandrei
  * since: 2018-09-05
  */
object KeyServiceClientRedisCacheUtil extends StrictLogging {

  /**
    * Caches a public key in Redis if necessary. The input result from a request to the `key-service` for a public key.
    *
    * @param publicKeyOpt public key to add to cache
    * @return the unchanged input after trying to cache it if necessary
    */
  def cachePublicKey(publicKeyOpt: Option[PublicKey])
                    (implicit system: ActorSystem, ec: ExecutionContextExecutor): Future[Option[PublicKey]] = {

    publicKeyOpt match {

      case None =>

        Future(None)

      case Some(result) =>

        val redis = RedisClientUtil.getRedisClient

        val expiry = expireInSeconds(result.pubKeyInfo)

        val pubKeyString = result.pubKeyInfo.pubKey
        val cacheKey = CacheHelperUtil.cacheKeyPublicKey(pubKeyString)
        val json = Json4sUtil.any2String(result).get
        redis.set[String](cacheKey, json, exSeconds = Some(expiry), NX = true) flatMap {

          case true =>

            logger.debug(s"cached public key: key=$cacheKey (expiry = $expiry seconds)")
            Future(Some(result))

          case false =>

            logger.error(s"failed to add to key-service rest client cache: key=$cacheKey")
            Future(Some(result))

        }

    }

  }

  /**
    * Caches a set of valid public keys in Redis if necessary. The input result from a request to the `key-service` for
    * all currently valid public keys.
    *
    * @param hardwareId      hardwareId the set of public keys belongs to
    * @param publicKeySetOpt set of public keys to add to cache
    * @return the unchanged input after trying to cache it if necessary
    */
  def cacheValidKeys(hardwareId: String, publicKeySetOpt: Option[Set[PublicKey]])
                    (implicit system: ActorSystem, ec: ExecutionContextExecutor): Future[Option[Set[PublicKey]]] = {

    publicKeySetOpt match {

      case None =>

        Future(None)

      case Some(result) =>

        val redis = RedisClientUtil.getRedisClient

        val expiry = expireInSeconds(result)

        val cacheKey = CacheHelperUtil.cacheKeyHardwareId(hardwareId)
        val json = Json4sUtil.any2String(result).get
        redis.set[String](cacheKey, json, exSeconds = Some(expiry), NX = true) flatMap {

          case true =>

            logger.debug(s"cached valid public keys: key=$cacheKey (expiry = $expiry seconds)")
            Future(Some(result))

          case false =>

            logger.error(s"failed to add to key-service rest client cache: key=$cacheKey")
            Future(Some(result))

        }

    }

  }

  /**
    * Caches a set of valid public keys in Redis if necessary. The input result from a request to the `key-service` for
    * all currently valid public keys.
    *
    * @param findTrustedSigned   the original request object
    *                            @param trustedKeys resulting trusted keys; might be a JsonErrorResponse, too
    * @return the unchanged input after trying to cache it if necessary
    */
  def cacheTrustedKeys(findTrustedSigned: FindTrustedSigned,
                       trustedKeys: Either[JsonErrorResponse, Set[TrustedKeyResult]]
                      )
                      (implicit system: ActorSystem, ec: ExecutionContextExecutor): Future[Either[JsonErrorResponse, Set[TrustedKeyResult]]] = {

    trustedKeys match {

      case Left(_) =>

        Future(trustedKeys)

      case Right(results) =>

        val sourcePubKey = findTrustedSigned.findTrusted.sourcePublicKey
        val depth = findTrustedSigned.findTrusted.depth
        val minTrust = findTrustedSigned.findTrusted.minTrustLevel
        val cacheKey = CacheHelperUtil.cacheKeyFindTrusted(sourcePubKey, depth, minTrust)

        val publicKeys = results map(_.publicKey)
        val expiry = expireInSeconds(publicKeys)

        val json = Json4sUtil.any2String(results).get
        val redis = RedisClientUtil.getRedisClient
        redis.set[String](cacheKey, json, exSeconds = Some(expiry), NX = true) flatMap {

          case true =>

            logger.debug(s"cached trusted public keys: key=$cacheKey (expiry = $expiry seconds)")
            Future(trustedKeys)

          case false =>

            logger.error(s"failed to add to key-service rest client cache: key=$cacheKey")
            Future(trustedKeys)

        }

    }

  }

  private def expireInSeconds(pubKeyInfo: PublicKeyInfo): Int = {

    val maxTTL = KeyClientRedisConfig.maxTTL
    val now = DateUtil.nowUTC
    val maxExpiryDate = now.plusSeconds(maxTTL)
    pubKeyInfo.validNotAfter match {

      case Some(validNotAfter) if validNotAfter.isBefore(maxExpiryDate.getMillis) =>

        (validNotAfter.getMillis - now.getMillis / 1000).toInt

      case _ =>

        maxTTL

    }

  }

  private def expireInSeconds(pubKeySet: Set[PublicKey]): Int = {

    val maxTTL = KeyClientRedisConfig.maxTTL

    if (pubKeySet.isEmpty) {

      maxTTL

    } else {

      val defaultValidNotAfter = DateUtil.nowUTC.getMillis + maxTTL
      val validNotAfterSet = pubKeySet.map(_.pubKeyInfo.validNotAfter).map {
        case None => defaultValidNotAfter
        case Some(validNotAfter) => validNotAfter.getMillis
      }
      val earliestValidNotAfter = validNotAfterSet.min

      if (earliestValidNotAfter >= defaultValidNotAfter) {

        maxTTL

      } else {

        val now = DateUtil.nowUTC.getMillis
        (earliestValidNotAfter - now / 1000).toInt

      }

    }

  }

}
