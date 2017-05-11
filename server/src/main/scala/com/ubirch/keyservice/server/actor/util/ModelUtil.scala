package com.ubirch.keyservice.server.actor.util

import com.ubirch.key.model.rest.PublicKey

/**
  * author: cvandrei
  * since: 2017-05-11
  */
object ModelUtil {

  def withPubKeyId(pubKey: PublicKey): PublicKey = {

    if (pubKey.pubKeyInfo.pubKeyId.isDefined) {

      pubKey

    } else {

      val info = pubKey.pubKeyInfo.copy(pubKeyId = Some(pubKey.pubKeyInfo.pubKey))
      pubKey.copy(pubKeyInfo = info)

    }

  }

}
