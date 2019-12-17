package com.ubirch.keyservice.util.pubkey

import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.Base64

import com.typesafe.scalalogging.slf4j.StrictLogging
import org.apache.commons.codec.binary.Hex
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.json.Json4sUtil
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.{Curve, Hash, Utils}

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

        val part: Array[Byte] = raw.charAt(3) match {
          case '2' => Hex.decodeHex(raw).dropRight(66)
          case  _ => Hex.decodeHex(raw).dropRight(67)
        }
        logger.debug("pubKey: '%s'".format(publicKey.pubKeyInfo.pubKey))
        logger.debug("signed part: '%s'".format(Hex.encodeHexString(part)))
        try {
          val pubKeyBytes = Base64.getDecoder.decode(publicKey.pubKeyInfo.pubKey)
          val pubKey = GeneratorKeyFactory.getPubKey(pubKeyBytes, associateCurve(publicKey.pubKeyInfo.algorithm))
          pubKey.verify(Utils.hash(part, associateHash(publicKey.pubKeyInfo.algorithm)), Base64.getDecoder.decode(publicKey.signature))
        } catch {
          case e: InvalidKeySpecException =>
            logger.error("failed to validate signature", e)
            false
        }
      case None =>
        publicKeyInfo2String(publicKey.pubKeyInfo) match {

          case Some(publicKeyInfoString) =>
            //TODO added prevPubKey signature check!!!
            logger.info(s"publicKeyInfoString: '$publicKeyInfoString'")
            try {
              val pubKeyBytes = Base64.getDecoder.decode(publicKey.pubKeyInfo.pubKey)
              val pubKey = GeneratorKeyFactory.getPubKey(pubKeyBytes, associateCurve(publicKey.pubKeyInfo.algorithm))
              pubKey.verify(publicKeyInfoString.getBytes, Base64.getDecoder.decode(publicKey.signature))
            } catch {
              case e: InvalidKeySpecException =>
                logger.error("failed to validate signature", e)
                false
            }

          case None => false

        }
    }
  }

  /**
    * Associate a string to a curve used by the crypto lib
    * @param curve the string representing the curve
    * @return the associated curve
    */
  def associateCurve(curve: String): Curve = {
    curve match {
      case "ecdsa-p256v1" => Curve.PRIME256V1
      case _ => Curve.Ed25519
    }
  }

  /**
    * Associate a string to a Hash algorithm used by the algorithm
    * @param curve the string representing the curve
    * @return the associated curve
    */
  def associateHash(curve: String): Hash = {
    curve match {
      case "ecdsa-p256v1" => Hash.SHA256
      case _ => Hash.SHA512
    }
  }

}
