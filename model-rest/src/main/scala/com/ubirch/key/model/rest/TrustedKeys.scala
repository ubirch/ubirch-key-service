package com.ubirch.key.model.rest

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-07
  */
case class TrustedKeys(depth: Int = 1,
                       minTrustLevel: Int = 10,
                       originatorPublicKey: String,
                       queryDate: DateTime = DateUtil.nowUTC
                      )

case class SignedTrustedKeys(signature: String,
                             trustedKeys: TrustedKeys
                            )
