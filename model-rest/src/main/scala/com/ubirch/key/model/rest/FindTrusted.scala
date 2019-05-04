package com.ubirch.key.model.rest

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-07
  */
// fields should be ordered alphabetically as some client libs only produce JSON with alphabetically ordered fields!!!
case class FindTrusted(curveAlgorithm: String,
                       depth: Int = 1,
                       minTrustLevel: Int = 50,
                       queryDate: DateTime = DateUtil.nowUTC,
                       sourcePublicKey: String
                      )

case class FindTrustedSigned(findTrusted: FindTrusted,
                             signature: String
                            )

case class TrustedKeyResult(depth: Int,
                            trustLevel: Int,
                            publicKey: PublicKey
                           )
