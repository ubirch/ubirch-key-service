package com.ubirch.keyservice.util.pubkey

import com.ubirch.crypto.ecc.EccUtil
import com.ubirch.key.model._
import com.ubirch.key.model.db.{PublicKey, PublicKeyInfo}
import com.ubirch.util.date.DateUtil
import com.ubirch.util.json.{Json4sUtil, JsonFormats}
import com.ubirch.util.uuid.UUIDUtil
import org.joda.time.format.ISODateTimeFormat
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.scalatest.{FeatureSpec, Matchers}

/**
  * author: cvandrei
  * since: 2018-03-15
  */
class PublicKeyUtilSpec extends FeatureSpec
  with Matchers {

  implicit val formats: Formats = JsonFormats.default

  private val dateTimeFormat = ISODateTimeFormat.dateTime()

  feature("publicKeyInfo2String()") {

    scenario("db.PublicKeyInfo with all fields set --> ensure alphabetical order of fields in JSON") {

      // prepare
      val (oldPublicKey, _) = EccUtil.generateEccKeyPairEncoded
      val (newPublicKey, _) = EccUtil.generateEccKeyPairEncoded
      val hardwareDeviceId = UUIDUtil.uuid

      val now = DateUtil.nowUTC
      val inSixMonths = now.plusMonths(6)
      val pubKeyInfo = PublicKeyInfo(
        algorithm = "ECC_ED25519",
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
      val (newPublicKey, _) = EccUtil.generateEccKeyPairEncoded
      val hardwareDeviceId = UUIDUtil.uuid

      val now = DateUtil.nowUTC
      val pubKeyUUID = UUIDUtil.uuidStr
      val pubKeyInfo = PublicKeyInfo(
        algorithm = "ECC_ED25519",
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

  feature("validateSignature()") {

    scenario("db.PublicKeyInfo with all fields set; db.PublicKey with _previousPubKeySignature_; valid signature --> true") {

      // prepare
      val (oldPublicKey, oldPrivateKey) = EccUtil.generateEccKeyPairEncoded
      val (newPublicKey, newPrivateKey) = EccUtil.generateEccKeyPairEncoded
      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val inSixMonths = now.plusMonths(6)
      val pubKeyInfo = PublicKeyInfo(
        algorithm = "ECC_ED25519",
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = Some(oldPublicKey),
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = Some(inSixMonths),
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = EccUtil.signPayload(newPrivateKey, pubKeyInfoString)
      val previousPubKeySignature = EccUtil.signPayload(oldPrivateKey, pubKeyInfoString)
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
      val (oldPublicKey, _) = EccUtil.generateEccKeyPairEncoded
      val (newPublicKey, newPrivateKey) = EccUtil.generateEccKeyPairEncoded
      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val inSixMonths = now.plusMonths(6)
      val pubKeyInfo = PublicKeyInfo(
        algorithm = "ECC_ED25519",
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = Some(oldPublicKey),
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = Some(inSixMonths),
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = EccUtil.signPayload(newPrivateKey, pubKeyInfoString)
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
      val (_, oldPrivateKey) = EccUtil.generateEccKeyPairEncoded
      val (newPublicKey, newPrivateKey) = EccUtil.generateEccKeyPairEncoded
      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val pubKeyInfo = PublicKeyInfo(
        algorithm = "ECC_ED25519",
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = None,
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = None,
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = EccUtil.signPayload(newPrivateKey, pubKeyInfoString)
      val previousPubKeySignature = EccUtil.signPayload(oldPrivateKey, pubKeyInfoString)
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
      val (newPublicKey, newPrivateKey) = EccUtil.generateEccKeyPairEncoded
      val hardwareDeviceId = UUIDUtil.uuid
      val newPublicKeyId = UUIDUtil.uuidStr

      val now = DateUtil.nowUTC
      val pubKeyInfo = PublicKeyInfo(
        algorithm = "ECC_ED25519",
        created = now,
        hwDeviceId = hardwareDeviceId.toString,
        previousPubKeyId = None,
        pubKey = newPublicKey,
        pubKeyId = newPublicKeyId,
        validNotAfter = None,
        validNotBefore = now
      )
      val pubKeyInfoString = PublicKeyUtil.publicKeyInfo2String(pubKeyInfo).get
      val signature = EccUtil.signPayload(newPrivateKey, pubKeyInfoString)
      val publicKey = PublicKey(
        pubKeyInfo = pubKeyInfo,
        signature = signature,
        previousPubKeySignature = None
      )

      // test & verify
      PublicKeyUtil.validateSignature(publicKey) shouldBe true

    }

    scenario("rest.PublicKey converted to db.PublicKey; valid signature based on rest.PublicKey --> true") {

      val (newPublicKey, newPrivateKey) = EccUtil.generateEccKeyPairEncoded
      val hardwareDeviceId = UUIDUtil.uuid

      val now = DateUtil.nowUTC
      val pubKeyInfoRest = rest.PublicKeyInfo(
        algorithm = "ECC_ED25519",
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
        signature = EccUtil.signPayload(newPrivateKey, pubKeyInfoString)
      )

      // test & verify
      PublicKeyUtil.validateSignature(publicKey) shouldBe true

    }

  }

}
