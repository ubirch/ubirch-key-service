## Local Setup

1. download and install [Neo4j 3.1.x](https://neo4j.com/download/community-edition/) (community edition)

1. start Neo4j

    1. first time setup: set the password to "neo4jneo4j" (as configured in application.base.conf)
  
        1. https://neo4j.com/docs/operations-manual/current/configuration/set-initial-password/

    1. prepare database
    
        *Running `dev-scripts/resetDatabase.sh` does everything in this step.*
  
        1. clear database
    
            `./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.Neo4jDelete"`
    
        1. create constraints

            `./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.InitData"`

1. start key-service

    ./sbt server/run

### Useful Cypher Queries

#### Delete

    MATCH (n)-[r]-(m) DELETE n, r, m // delete all nodes with a relationship to a nother node
    MATCH (n) DELETE n // all nodes
    MATCH (n: PublicKey) DELETE n // all public keys

#### List

    MATCH (pubKey: PublicKey) RETURN pubKey // all public keys
