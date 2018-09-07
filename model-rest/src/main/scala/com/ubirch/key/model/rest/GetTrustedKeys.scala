package com.ubirch.key.model.rest

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-07
  */
case class GetTrustedKeys(created: DateTime = DateUtil.nowUTC,
                          depth: Int = 1,
                          key: String
                         )

case class SignedGetTrustedKeys(getTrustedKeys: GetTrustedKeys,
                                signature: String
                               )

case class TrustedKeysResult(trustedKeys: Set[PublicKey])
