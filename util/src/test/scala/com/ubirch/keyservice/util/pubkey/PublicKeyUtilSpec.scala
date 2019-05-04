package com.ubirch.keyservice.util.pubkey

import java.util.Base64

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.key.model._
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.date.DateUtil
import com.ubirch.keyservice.util.pubkey.PublicKeyUtil.associateCurve
import com.ubirch.util.json.{Json4sUtil, JsonFormats}
import com.ubirch.util.uuid.UUIDUtil
import org.apache.commons.codec.binary.Hex
import org.joda.time.format.ISODateTimeFormat
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

/**
  * author: cvandrei
  * since: 2018-03-15
  */
class PublicKeyUtilSpec extends FeatureSpec with Matchers with GivenWhenThen {

  val ECDSA: String = "ecdsa-p256v1"
  val EDDSA: String = "ed25519-sha-512"

  implicit val formats: Formats = JsonFormats.default

  private val dateTimeFormat = ISODateTimeFormat.dateTime()

  def publicKeyInfo2String(curveName: String): Unit = {
    scenario("db.PublicKeyInfo with all fields set --> ensure alphabetical order of fields in JSON") {

      // prepare
      val oldPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val oldPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(oldPrivKey.getRawPublicKey))
      val newPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(newPrivKey.getRawPublicKey))
      val hardwareDeviceId = UUIDUtil.uuid

      val now = DateUtil.nowUTC
      val inSixMonths = now.plusMonths(6)
      val pubKeyInfo = PublicKeyInfo(
        algorithm = curveName,
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = Some(oldPublicKey),
        pubKey = newPublicKey,
        pubKeyId = newPublicKey,
        validNotAfter = Some(inSixMonths),
        validNotBefore = now
      )

      val nowString = dateTimeFormat.print(now)
      val inSixMonthsString = dateTimeFormat.print(inSixMonths)
      val expected = s"""{"algorithm":"${pubKeyInfo.algorithm}","created":"$nowString","hwDeviceId":"$hardwareDeviceId","previousPubKeyId":"$oldPublicKey","pubKey":"$newPublicKey","pubKeyId":"$newPublicKey","validNotAfter":"$inSixMonthsString","validNotBefore":"$nowString"}"""

      // test & verify
      PublicKeyUtil.publicKeyInfo2String(pubKeyInfo) shouldBe Some(expected)

    }

    scenario("db.PublicKeyInfo with only mandatory fields set --> ensure alphabetical order of fields in JSON") {

      // prepare
      val newPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(newPrivKey.getRawPublicKey))
      val hardwareDeviceId = UUIDUtil.uuid

      val now = DateUtil.nowUTC
      val pubKeyUUID = UUIDUtil.uuidStr
      val pubKeyInfo = PublicKeyInfo(
        algorithm = curveName,
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = None,
        pubKey = newPublicKey,
        pubKeyId = pubKeyUUID,
        validNotAfter = None,
        validNotBefore = now
      )

      val nowString = dateTimeFormat.print(now)
      val expected = s"""{"algorithm":"${pubKeyInfo.algorithm}","created":"$nowString","hwDeviceId":"$hardwareDeviceId","pubKey":"$newPublicKey","pubKeyId":"$pubKeyUUID","validNotBefore":"$nowString"}"""

      // test & verify
      PublicKeyUtil.publicKeyInfo2String(pubKeyInfo) shouldBe Some(expected)

    }

  }

  feature("publicKeyInfo2String() - ECDSA") {
    scenariosFor(publicKeyInfo2String(ECDSA))
  }

  feature("publicKeyInfo2String() - EDDSA") {
    scenariosFor(publicKeyInfo2String(EDDSA))
  }

  def validateSignature(curveName: String): Unit = {
    scenario("db.PublicKeyInfo with all fields set; db.PublicKey with _previousPubKeySignature_; valid signature --> true") {

      // prepare
      val oldPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val oldPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(oldPrivKey.getRawPublicKey))
      val newPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(newPrivKey.getRawPublicKey))

      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val inSixMonths = now.plusMonths(6)
      val pubKeyInfo = PublicKeyInfo(
        algorithm = curveName,
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = Some(oldPublicKey),
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = Some(inSixMonths),
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = Base64.getEncoder.encodeToString(newPrivKey.sign(pubKeyInfoString.getBytes))
      val previousPubKeySignature = Base64.getEncoder.encodeToString(oldPrivKey.sign(pubKeyInfoString.getBytes))
      val publicKey = PublicKey(
        pubKeyInfo = pubKeyInfo,
        signature = signature,
        previousPubKeySignature = Some(previousPubKeySignature)
      )

      // test & verify
      PublicKeyUtil.validateSignature(publicKey) shouldBe true

    }

    scenario("db.PublicKeyInfo with all fields set; db.PublicKey without _previousPubKeySignature_; valid signature --> true") {

      // prepare
      val oldPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val oldPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(oldPrivKey.getRawPublicKey))
      val newPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(newPrivKey.getRawPublicKey))

      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val inSixMonths = now.plusMonths(6)
      val pubKeyInfo = PublicKeyInfo(
        algorithm = curveName,
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = Some(oldPublicKey),
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = Some(inSixMonths),
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = Base64.getEncoder.encodeToString(newPrivKey.sign(pubKeyInfoString.getBytes))
      val publicKey = PublicKey(
        pubKeyInfo = pubKeyInfo,
        signature = signature,
        previousPubKeySignature = None
      )

      // test & verify
      PublicKeyUtil.validateSignature(publicKey) shouldBe true

    }

    scenario("db.PublicKeyInfo with only mandatory fields set; db.PublicKey with _previousPubKeySignature_; valid signature --> true") {

      // prepare
      val oldPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(newPrivKey.getRawPublicKey))

      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val pubKeyInfo = PublicKeyInfo(
        algorithm = curveName,
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = None,
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = None,
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = Base64.getEncoder.encodeToString(newPrivKey.sign(pubKeyInfoString.getBytes))// EccUtil.signPayload(newPrivateKey, pubKeyInfoString)
      val previousPubKeySignature = Base64.getEncoder.encodeToString(oldPrivKey.sign(pubKeyInfoString.getBytes)) //EccUtil.signPayload(oldPrivateKey, pubKeyInfoString)
      val publicKey = PublicKey(
        pubKeyInfo = pubKeyInfo,
        signature = signature,
        previousPubKeySignature = Some(previousPubKeySignature)
      )

      // test & verify
      PublicKeyUtil.validateSignature(publicKey) shouldBe true

    }

    scenario("db.PublicKeyInfo with only mandatory fields set; db.PublicKey without _previousPubKeySignature_; valid signature --> true") {

      // prepare
      val newPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(newPrivKey.getRawPublicKey))
      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val pubKeyInfo = PublicKeyInfo(
        algorithm = curveName,
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = None,
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = None,
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = Base64.getEncoder.encodeToString(newPrivKey.sign(pubKeyInfoString.getBytes))
      val publicKey = PublicKey(
        pubKeyInfo = pubKeyInfo,
        signature = signature,
        previousPubKeySignature = None
      )

      // test & verify
      PublicKeyUtil.validateSignature(publicKey) shouldBe true

    }

    scenario("rest.PublicKey converted to db.PublicKey; valid signature based on rest.PublicKey --> true") {

      val newPrivKey = GeneratorKeyFactory.getPrivKey(associateCurve(curveName))
      val newPublicKey = Base64.getEncoder.encodeToString(Hex.decodeHex(newPrivKey.getRawPublicKey))
      val hardwareDeviceId = UUIDUtil.uuid

      val now = DateUtil.nowUTC
      val pubKeyInfoRest = rest.PublicKeyInfo(
        algorithm = curveName,
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = None,
        pubKey = newPublicKey,
        pubKeyId = newPublicKey,
        validNotAfter = None,
        validNotBefore = now
      )
      val pubKeyInfoString = write(pubKeyInfoRest)

      val publicKey = PublicKey(
        pubKeyInfo = Json4sUtil.any2any[db.PublicKeyInfo](pubKeyInfoRest),
        signature = Base64.getEncoder.encodeToString(newPrivKey.sign(pubKeyInfoString.getBytes))
      )

      // test & verify
      PublicKeyUtil.validateSignature(publicKey) shouldBe true

    }
  }

  feature("validateSignature() - ECDSA") {
    scenariosFor(validateSignature(ECDSA))
  }

  feature("validateSignature() - EDDSA") {
    scenariosFor(validateSignature(EDDSA))
  }

}
