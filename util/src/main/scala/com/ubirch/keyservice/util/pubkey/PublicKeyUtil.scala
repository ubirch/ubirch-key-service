package com.ubirch.keyservice.util.pubkey

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.json.Json4sUtil

/**
  * Created by derMicha on 19/05/17.
  */
object PublicKeyUtil {

  def publicKeyInfo2String(publicKeyInfo: PublicKeyInfo): Option[String] = {
    Json4sUtil.any2jvalue(publicKeyInfo) match {
      case Some(json) =>
        val string = Json4sUtil.jvalue2String(json)
        Some(string)
      case None => None
    }
  }

  def checkPublicKey(publicKey: PublicKey): Boolean = {

    val publicKeyInfo = publicKey.pubKeyInfo
    publicKeyInfo2String(publicKeyInfo) match {
      case Some(publicKeyInfoString) =>
        //TODO added prevPubKey signature check!!!
        EccUtil.validateSignature(publicKeyInfo.pubKey, publicKey.signature, publicKeyInfoString)
      case None =>
        false
    }
  }

}
