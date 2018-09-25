package com.ubirch.key.model.db

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-17
  */
case class SignedRevoke(revokation: Revokation,
                        signature: String
                       )

// fields should be ordered alphabetically as some client libs only produce JSON with alphabetically ordered fields!!!
case class Revokation(publicKey: String,
                      revokationDate: DateTime = DateUtil.nowUTC
                     )
