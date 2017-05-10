package com.ubirch.keyservice.util.neo4j

/**
  * author: cvandrei
  * since: 2017-05-10
  */
object Neo4jConstraints {

  val constraintPubKeyIdUnique = "CONSTRAINT ON (pubKey:PublicKey) ASSERT pubKey.infoPubKeyId IS UNIQUE"

  val constraints = Set(
    constraintPubKeyIdUnique
  )

}
