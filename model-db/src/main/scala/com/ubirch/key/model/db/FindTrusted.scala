package com.ubirch.key.model.db

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-14
  */
case class FindTrusted(depth: Int = 1,
                       minTrustLevel: Int = 50,
                       sourcePublicKey: String,
                       queryDate: DateTime = DateUtil.nowUTC
                      )

case class FindTrustedSigned(findTrusted: FindTrusted,
                             signature: String
                            )

case class TrustedKeyResult(depth: Int,
                            trustLevel: Int,
                            publicKey: PublicKey
                           )
