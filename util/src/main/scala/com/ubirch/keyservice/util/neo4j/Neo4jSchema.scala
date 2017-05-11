package com.ubirch.keyservice.util.neo4j

/**
  * author: cvandrei
  * since: 2017-05-10
  */
object Neo4jSchema {

  val constraintPubKeyIdUnique = "CONSTRAINT ON (pubKey:PublicKey) ASSERT pubKey.infoPubKeyId IS UNIQUE"

  val constraints = Set(
    constraintPubKeyIdUnique
  )

  val indexHardwareId = "INDEX ON :PublicKey(infoHwDeviceId)"

  val indices = Set(
    indexHardwareId
  )

}
