package com.ubirch.keyservice.util.neo4j

/**
  * author: cvandrei
  * since: 2017-05-10
  */
object Neo4jSchema {

  val constraintPubKeyUnique = "CONSTRAINT ON (pubKey:PublicKey) ASSERT pubKey.infoPubKey IS UNIQUE"
  val constraintPubKeyIdUnique = "CONSTRAINT ON (pubKey:PublicKey) ASSERT pubKey.infoPubKeyId IS UNIQUE"

  val constraints = Set(
    constraintPubKeyUnique,
    constraintPubKeyIdUnique
  )

  val indexHardwareId = "INDEX ON :PublicKey(infoHwDeviceId)"

  val indices = Set(
    indexHardwareId
  )

}
