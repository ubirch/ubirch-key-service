## Release History

### Version 0.3.1 (tbd)

* bugfix: fields in the `rest.PublicKeyInfo` need to be alphabetical or otherwise signature verification fails (order of
fields needs to be same as in `db.PublicKeyInfo`)
* field `rest.PublicKeyInfo.pubKeyId` is now mandatory (as it already was in `rest.PublicKeyInfo`)

### Version 0.3.0 (2018-03-09)

* added endpoint `DELETE /pubkey`
* added `KeyServiceClientRest.pubKeyDELETE()`

### Version 0.2.2 (2018-03-08)

* update to `com.ubirch.util:config:0.2.0`
* update to `com.ubirch.util:date:0.5.1`

### Version 0.2.1 (2018-01-22)

* begin list of valid algorithms
* fix wrong server port in REST methods doc
* reduce code duplication by using existing code from `PublicKeyUtil`
* moved some of README's documentation to separate files in newly created folder _docs_
* improved documentation of method: _POST /pubkey_
* `PublicKeyUtil.validateSignature` catches `InvalidKeySpecException` now
* added endpoint `/api/keyService/v1/pubkey/$PUB_KEY`
* update to `com.ubirch.util:crypto:0.4.2`

### Version 0.2.0 (2017-07-28)

* refactor `KeyServiceClientRest.deepCheck` to return `DeepCheckResponse` (without Option)
* refactor `KeyServiceClientRest.deepCheck` responses to include a `[key-service]` prefix in all it's messages

### Version 0.1.13 (2017-07-27)

* update to `com.ubirch.util:json:0.4.3`
* update to `com.ubirch.util:deep-check-model:0.1.3`
* update to `com.ubirch.util:response-util:0.2.4`

### Version 0.1.12 (2017-07-24)

* revert to Akka 2.4.19
* revert to Play 2.4.11
* revert to anormcypher 0.9.1
* refactor `KeyServiceClientRest` to use Akka Http for the connection

### Version 0.1.11 (2017-07-18)

* add `resetDatabase.sh` script
* update to Akka 2.4.19
* update to Play 2.5.3
* update to anormcypher 0.10.0

### Version 0.1.10 (2017-07-17)

* update Akka HTTP to 10.0.9
* update _com.ubirch.util:rest-akka-http(-test)_ to 0.3.8
* update _com.ubirch.util:response-util_ to 0.2.3

### Version 0.1.9 (2017-07-17)

* extract `Neo4jSchema` and `Neo4jUtils` to new artifact `com.ubirch.key:utils-neo4j`

### Version 0.1.8 (2017-07-12)

* update logging dependencies
* update logback configs

### Version 0.1.7 (2017-06-29)

* refactored actors by adding a `props()` method
* updated to _com.ubirch.util:json:0.4.2_ and all ubirch util libs depending on it, too

### Version 0.1.6 (2017-06-21)

* add module `client-rest`

### Version 0.1.5 (2017-06-19)

* queries in `PublicKeyManager` are now asynchronous
* update json4s to version 3.5.2
* update to _com.ubirch.util:deep-check-model:0.1.1_
* update to _com.ubirch.util:json:0.4.1_
* update to _com.ubirch.util:response-util:0.2.1_

### Version 0.1.4 (2017-06-12)

* endpoint `/api/userService/v1/deepCheck` responds with http status 503 if deep check finds problems

### Version 0.1.3 (2017-06-09)

* migrate to _com.ubirch.util:deep-check-model:0.1.0_

### Version 0.1.2 (2017-06-08)

* introduce endpoint `/api/authService/v1/check`
* update to sbt 0.13.15
* added `PublicKeyUtil`
* update _com.ubirch.util:json_ to version 0.4.0
* update _com.ubirch.util:response-util_ to version 0.1.6
* introduce endpoint `/api/userService/v1/deepCheck`

### Version 0.1.1 (2017-05-18)

* update Akka Http to 10.0.6
* update Akka to 2.4.18

### Version 0.1.0 (2017-05-11)

* initial release
