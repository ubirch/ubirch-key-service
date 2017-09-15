package com.ubirch.keyservice.util.pubkey

import java.security.spec.InvalidKeySpecException

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.json.Json4sUtil

/**
  * Created by derMicha on 19/05/17.
  */
object PublicKeyUtil extends StrictLogging {

  def publicKeyInfo2String(publicKeyInfo: PublicKeyInfo): Option[String] = {
    Json4sUtil.any2jvalue(publicKeyInfo) match {
      case Some(json) =>
        val string = Json4sUtil.jvalue2String(json)
        Some(string)
      case None => None
    }
  }

  def validateSignature(publicKey: PublicKey): Boolean = {

    val publicKeyInfo = publicKey.pubKeyInfo
    publicKeyInfo2String(publicKeyInfo) match {

      case Some(publicKeyInfoString) =>
        //TODO added prevPubKey signature check!!!
        logger.debug(s"publicKeyInfoString: '$publicKeyInfoString'")
        try {
          EccUtil.validateSignature(publicKeyInfo.pubKey, publicKey.signature, publicKeyInfoString)
        } catch {
          case e: InvalidKeySpecException =>
            logger.error("failed to validate signature", e)
            false
        }

      case None => false

    }

  }

}
