## REST Methods

### Welcome / Health / Check

    curl localhost:8095/
    curl localhost:8095/api/keyService/v1
    curl localhost:8095/api/keyService/v1/check

If healthy the server response is:

    200 {"version":"1.0","status":"OK","message":"Welcome to the ubirchKeyService ( $GO_PIPELINE_NAME / $GO_PIPELINE_LABEL / $GO_PIPELINE_REVISION )"}

If not healthy the server response is:

    400 {"version":"1.0","status":"NOK","message":"$ERROR_MESSAGE"}

### Deep Check / Server Health

    curl localhost:8095/api/keyService/v1/deepCheck

If healthy the response is:

  F  200 {"status":true,"messages":[]}

If not healthy the status is `false` and the `messages` array not empty:

    503 {"status":false,"messages":["unable to connect to the database"]}


### Public Key

#### Create

    curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '{
      "pubKeyInfo": {
        "hwDeviceId": "some-id-asdf", // String (not always a UUID)
        "pubKey": "string", // base64
        "pubKeyId": "string", // (optional) typically the hash of "pubKey" (algorithm tends to depend on "algorithm") but can also be the _pubKey_, too (useful for ECC keys whose hash would otherwise be longer than the actual key)
        "algorithm": "RSA4096", // check X.509 re: constants
        "previousPubKeyId": "string", // (optional) id of previous pub key
        "created": "2017-04-26T17:18:00.000Z+02:00",
        "validNotBefore": "2017-04-26T17:18:00.000Z+02:00",
        "validNotAfter": "2019-04-26T17:18:00.000Z+02:00" // (optional)
      },
      "signature": "string", // base64 (self signed)
      "previousPubKeySignature": "..." // (optional)
    }'

Valid _algorithm_s are:

* RSA4096
* ECC_ED25519

If successful the response is:

    200
    {
      "pubKeyInfo": {
        "hwDeviceId": "some-id-asdf", // String (not always a UUID)
        "pubKey": "string", // base64
        "pubKeyId": "string", // (optional) typically the hash of "pubKey" (algorithm tends to depend on "algorithm") but can also be the _pubKey_, too (useful for ECC keys whose hash would otherwise be longer than the actual key)
        "algorithm": "RSA4096", // check X.509 re: constants
        "previousPubKeyId": "...", // (optional) String - pub key id
        "created": "2017-04-26T17:18:00.000Z+02:00",
        "validNotBefore": "2017-04-26T17:18:00.000Z+02:00",
        "validNotAfter": "2019-04-26T17:18:00.000Z+02:00" // (optional)
      },
      "signature": "string", // base64 (self signed)
      "previousPubKeySignature": "..." // (optional)
    }

In case of an error the response is:

    400
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "CreateError",
        "errorMessage": "failed to create public key"
      }
    }

If the server has problems the response is:

    500
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "ServerError",
        "errorMessage": "failed to create public key"
      }
    }

#### Query Public Keys by HardwareId (currently active only)

    curl localhost:8095/api/keyService/v1/pubkey/current/hardwareId/$HARDWARE_ID

If no currently valid public keys were found the response is:

    200
    []

If currently valid public keys were found the response is:

    200
    [
      {
        "pubKeyInfo": {
          "hwDeviceId": "some-id-asdf", // String (not always a UUID)
          "pubKey": "string", // base64
          "pubKeyId": "string", // (optional) typically the hash of "pubKey" (algorithm tends to depend on "algorithm") but can also be the _pubKey_, too (useful for ECC keys whose hash would otherwise be longer than the actual key)
          "algorithm": "RSA4096", // check X.509 re: constants
          "previousPubKeyId": "...", // (optional) pub key id
          "created": "2017-04-26T17:18:00.000Z+02:00",
          "validNotBefore": "2017-04-26T17:18:00.000Z+02:00",
          "validNotAfter": "2019-04-26T17:18:00.000Z+02:00" // (optional)
        },
        "signature": "string", // base64 (self signed)
        "previousPubKeySignature": "..." // (optional)
      }
    ]

In case of an error the response is:

    400
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "QueryError",
        "errorMessage": "failed to query public keys"
      }
    }

If the server has problems the response is:

    500
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "ServerError",
        "errorMessage": "failed to query public keys"
      }
    }