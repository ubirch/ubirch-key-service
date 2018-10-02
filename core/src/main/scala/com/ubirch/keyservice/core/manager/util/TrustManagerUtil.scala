package com.ubirch.keyservice.core.manager.util

import com.ubirch.key.model.db.TrustedKeyResult
import com.ubirch.keyservice.config.KeySvcConfig

import scala.annotation.tailrec

/**
  * author: cvandrei
  * since: 2018-10-01
  */
object TrustManagerUtil {

  def maxWebOfTrustDepth(desiredDepth: Int): Int = Set(desiredDepth, KeySvcConfig.searchTrustedKeysMaxDepth).min

  @tailrec
  def extractTrustedKeys(trustPaths: Seq[Seq[TrustPath]])(implicit knownTrustedKeys: Set[TrustedKeyResult] = Set.empty): Set[TrustedKeyResult] = {

    if (trustPaths.isEmpty) {
      knownTrustedKeys
    } else {

      val knownTrustedKeysUpdate = extractTrustedKeysFromSinglePath(trustPaths.head, knownTrustedKeys)

      trustPaths.tail match {
        case Nil => knownTrustedKeysUpdate
        case tail => extractTrustedKeys(tail)(knownTrustedKeysUpdate)
      }

    }

  }

  def extractTrustedKeysFromSinglePath(trustPaths: Seq[TrustPath], knownTrustedKeys: Set[TrustedKeyResult]): Set[TrustedKeyResult] = {

    var updatedTrustedKeys = knownTrustedKeys
    trustPaths.zipWithIndex foreach {

      case (pathSegment, depthIndex) =>

        if (containsUnknownTrustedKey(pathSegment, updatedTrustedKeys)) {

          val anotherTrustedKey = TrustedKeyResult(
            depth = depthIndex + 1,
            trustLevel = pathSegment.signedTrust.trustRelation.trustLevel,
            publicKey = pathSegment.to
          )
          updatedTrustedKeys += anotherTrustedKey

        }

    }
    updatedTrustedKeys

  }

  /**
    * @param pathSegment current trust path segment for which to decide if it's `to` public key is a trusted key we don't know about yet
    * @return true if the current path segment of the trust path
    */
  def containsUnknownTrustedKey(pathSegment: TrustPath, knownTrustedKeys: Set[TrustedKeyResult]): Boolean = {

    !knownTrustedKeys.map(_.publicKey).contains(pathSegment.to)

  }

}
