package com.ubirch.key.model.rest

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-06
  */
case class SignedTrustRelation(signature: String, trustRelation: TrustRelation)

// fields should be ordered alphabetically as some client libs only produce JSON with alphabetically ordered fields!!!
case class TrustRelation(created: DateTime = DateUtil.nowUTC,
                         sourcePublicKey: String,
                         targetPublicKey: String,
                         trustLevel: Int = 10,
                         validNotAfter: Option[DateTime] = None
                        )
