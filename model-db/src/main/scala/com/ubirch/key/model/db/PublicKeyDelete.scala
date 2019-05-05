package com.ubirch.key.model.db

/**
  * author: cvandrei
  * since: 2018-03-09
  */

/**
  * @param publicKey base64 encoded public key
  * @param signature base64 encoded signature of field _publicKey_
  */
case class PublicKeyDelete(publicKey: String, signature: String)
