# ubirch-key-service


## General Information

This project serves as a keyserver but unlike PGP keyservers it has additional features (for example, uploading pub keys
is only possible if you control the private key, too).


## Configuration

| Config Item                      | Mandatory  | Description                           |
|:---------------------------------|:-----------|:--------------------------------------|
| ubirchKeyService.neo4j.host      | no         | Neo4j host (defaults to "localhost")  |
| ubirchKeyService.neo4j.port      | no         | Neo4j port (defaults to 7474)         |
| ubirchKeyService.neo4j.userName  | no         | Neo4j user name (defaults to "")      |
| ubirchKeyService.neo4j.password  | no         | Neo4j password (defaults to "")       |
| ubirchKeyService.neo4j.https     | no         | Neo4j password (defaults to "")       |


## Deployment Notes

This service has the following dependencies:

* Neo4j 3.3.x (constraints and indices are created during server startup)


## Automated Tests

run all tests

    ./sbt test

Most tests are in the following modules:

* `core`
* `client-rest`

### generate coverage report

    ./sbt coverageReport

more details here: https://github.com/scoverage/sbt-scoverage


## Create Docker Image

    ./goBuild.sh assembly
