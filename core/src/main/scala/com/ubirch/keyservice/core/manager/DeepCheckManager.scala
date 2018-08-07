package com.ubirch.keyservice.core.manager

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.util.deepCheck.model.DeepCheckResponse
import com.ubirch.util.deepCheck.util.DeepCheckResponseUtil

import org.neo4j.driver.v1.{Driver, Transaction, TransactionWork}

/**
  * author: cvandrei
  * since: 2017-06-08
  */
object DeepCheckManager extends StrictLogging {

  def connectivityCheck()(implicit neo4jDriver: Driver): DeepCheckResponse = {

    val res = try {

      val query = "MATCH (n) RETURN n LIMIT 1"

      val session = neo4jDriver.session
      try {

        // TODO refactor: readTransactionAsync()
        session.readTransaction(new TransactionWork[DeepCheckResponse]() {
          def execute(tx: Transaction): DeepCheckResponse = {

            val result = tx.run(query)
            DeepCheckResponse()

          }
        })

      } finally if (session != null) session.close()

    } catch {

      case t: Throwable =>
        DeepCheckResponse(
          status = false,
          messages = Seq(t.getMessage)
        )

    }

    DeepCheckResponseUtil.addServicePrefix("key-service", res)

  }

}
