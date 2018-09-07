package com.ubirch.key.model.rest

import com.ubirch.util.date.DateUtil

import org.joda.time.DateTime

/**
  * author: cvandrei
  * since: 2018-09-07
  */
case class SignedRevoke(revoke: Revoke,
                        signature: String
                       )

// fields should be ordered alphabetically as some client libs only produce JSON with alphabetically ordered fields!!!
case class Revoke(created: DateTime = DateUtil.nowUTC,
                  key: String
                 )
