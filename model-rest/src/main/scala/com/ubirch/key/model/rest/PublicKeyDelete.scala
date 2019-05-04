package com.ubirch.key.model.rest

/**
  * author: cvandrei
  * since: 2018-03-08
  */

/**
  * @param curveAlgorithm algorithm used to generate public key
  * @param publicKey base64 encoded public key
  * @param signature base64 encoded signature of field _publicKey_
  */
case class PublicKeyDelete(curveAlgorithm: String,
                           publicKey: String,
                           signature: String
                          )
