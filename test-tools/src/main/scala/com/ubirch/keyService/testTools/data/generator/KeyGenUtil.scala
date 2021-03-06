package com.ubirch.keyService.testTools.data.generator

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.key.model.rest.{PublicKey, PublicKeyInfo}
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.Json4sUtil
import com.ubirch.util.uuid.UUIDUtil

/**
  * author: cvandrei
  * since: 2018-09-12
  */
object KeyGenUtil {

  def keyMaterial(publicKey: String, privateKey: String, algorithmCurve: String ): KeyMaterial = {

    val info = PublicKeyInfo(
      algorithm = algorithmCurve,
      created = DateUtil.nowUTC.minusHours(1),
      hwDeviceId = UUIDUtil.uuidStr,
      pubKey = publicKey,
      pubKeyId = publicKey,
      validNotBefore = DateUtil.nowUTC.minusMinutes(1)
    )
    val privKeyB64: Array[Byte] = Base64.getDecoder.decode(privateKey)
    val privKey = GeneratorKeyFactory.getPrivKey(privKeyB64, associateCurve(algorithmCurve))
    val signature = Base64.getEncoder.encodeToString(privKey.sign(Json4sUtil.any2String(info).get.getBytes))
    val publicKeyObject = PublicKey(info, signature)

    KeyMaterial(privateKey, publicKeyObject)

  }

  def keyMaterialDb(publicKey: String, privateKey: String, algorithmCurve: String): KeyMaterialDb = {

    val info = com.ubirch.key.model.db.PublicKeyInfo(
      algorithm = algorithmCurve,
      created = DateUtil.nowUTC.minusHours(1),
      hwDeviceId = UUIDUtil.uuidStr,
      pubKey = publicKey,
      pubKeyId = publicKey,
      validNotBefore = DateUtil.nowUTC.minusMinutes(1)
    )
    val privKey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode(privateKey), associateCurve(algorithmCurve))
    val signature = privKey.sign(Json4sUtil.any2String(info).get.getBytes)
    val publicKeyObject = com.ubirch.key.model.db.PublicKey(info, Base64.getEncoder.encodeToString(signature))

    KeyMaterialDb(privateKey, publicKeyObject)

  }

}
