package com.ubirch.key.model.rest

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-06
  */
case class SignedTrustRelation(trustRelation: TrustRelation,
                               signature: String
                              )

// fields should be ordered alphabetically as some client libs only produce JSON with alphabetically ordered fields!!!
case class TrustRelation(created: DateTime = DateUtil.nowUTC,
                         sourcePublicKey: String,
                         targetPublicKey: String,
                         trustLevel: Int = 50, // value range: 1, ..., 100 (higher values have more weight)
                         validNotAfter: Option[DateTime] = None
                        )
