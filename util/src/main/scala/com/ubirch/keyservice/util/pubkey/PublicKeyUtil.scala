package com.ubirch.keyservice.util.pubkey

import java.security.spec.InvalidKeySpecException

import com.typesafe.scalalogging.slf4j.StrictLogging
import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.crypto.hash.HashUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.json.Json4sUtil
import org.apache.commons.codec.binary.Hex
import com.roundeights.hasher.Implicits._
import com.roundeights.hasher.{Digest, Hash}

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

    publicKey.raw match {
      case Some(raw) =>
        logger.debug(s"rawMessage: '$raw'")
        val part: Array[Byte] = Hex.decodeHex(raw).dropRight(67)
        logger.debug("pubKey: '%s'".format(publicKey.pubKeyInfo.pubKey))
        logger.debug("signed part: '%s'".format(Hex.encodeHexString(part)))
        try {
          EccUtil.validateSignatureSha512(publicKey.pubKeyInfo.pubKey, publicKey.signature, part)
        } catch {
          case e: InvalidKeySpecException =>
            logger.error("failed to validate signature", e)
            false
        }
      case None =>
        publicKeyInfo2String(publicKey.pubKeyInfo) match {

          case Some(publicKeyInfoString) =>
            //TODO added prevPubKey signature check!!!
            logger.debug(s"publicKeyInfoString: '$publicKeyInfoString'")
            try {
              EccUtil.validateSignature(publicKey.pubKeyInfo.pubKey, publicKey.signature, publicKeyInfoString)
            } catch {
              case e: InvalidKeySpecException =>
                logger.error("failed to validate signature", e)
                false
            }

          case None => false

        }
    }
  }
}
