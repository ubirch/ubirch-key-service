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

    curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '
    {
      "pubKeyInfo": {
        "hwDeviceId": "4150473037-547268290-3238389072-173590267", // String (not always a UUID)
        "pubKey": "MC0wCAYDK2VkCgEBAyEAovEmQJuiWdrb5hV/mhG1SF9Vul7tRveYZ74Mk+Okjhg=", // base64
        "pubKeyId": "MC0wCAYDK2VkCgEBAyEAovEmQJuiWdrb5hV/mhG1SF9Vul7tRveYZ74Mk+Okjhg=",  // (optional) Base64
        "algorithm": "ECC_ED25519", // see list of valid algorithms below
        "previousPubKeyId": "MC0wCAYDK2VkCgEBAyEAovEmQJuiWdrb5hV/mhG1SF9Vul7tRveYZ74Mk+Okjhg=",  // (optional) Base64 encoded id of previous pub key 
        "created": "2017-08-03T09:51:36.000Z",
        "validNotBefore": "2017-08-03T09:51:36.000Z",
        "validNotAfter": "2018-02-03T09:51:36.000Z" // (optional)
      },
      "signature": "MfIJEmhbIQBwHK4URdqialGOyeg1ZKyIAGPmy5VZ8Cfim4hnu3c4SAzHdhHuu4UY0XP3BWgPRVXmf8/mv8s3Dw==", // Base64 (self signed)
      "previousPubKeySignature": "MfIJEmhbIQBwHK4URdqialGOyeg1ZKyIAGPmy5VZ8Cfim4hnu3c4SAzHdhHuu4UY0XP3BWgPRVXmf8/mv8s3Dw==" // (optional) Base64 (self signed)
    }'

Valid _algorithm_s are:

* RSA4096
* ECC_ED25519

If successful the response is exactly the key from the request:

    200
    {
      "pubKeyInfo": {
        "hwDeviceId": "4150473037-547268290-3238389072-173590267",
        "pubKey": "MC0wCAYDK2VkCgEBAyEAovEmQJuiWdrb5hV/mhG1SF9Vul7tRveYZ74Mk+Okjhg=",
        "pubKeyId": "MC0wCAYDK2VkCgEBAyEAovEmQJuiWdrb5hV/mhG1SF9Vul7tRveYZ74Mk+Okjhg=",
        "algorithm": "ECC_ED25519",
        "previousPubKeyId": "MC0wCAYDK2VkCgEBAyEAovEmQJuiWdrb5hV/mhG1SF9Vul7tRveYZ74Mk+Okjhg=", 
        "created": "2017-08-03T09:51:36.000Z",
        "validNotBefore": "2017-08-03T09:51:36.000Z",
        "validNotAfter": "2018-02-03T09:51:36.000Z"
      },
      "signature": "MfIJEmhbIQBwHK4URdqialGOyeg1ZKyIAGPmy5VZ8Cfim4hnu3c4SAzHdhHuu4UY0XP3BWgPRVXmf8/mv8s3Dw==",
      "previousPubKeySignature": "MfIJEmhbIQBwHK4URdqialGOyeg1ZKyIAGPmy5VZ8Cfim4hnu3c4SAzHdhHuu4UY0XP3BWgPRVXmf8/mv8s3Dw=="
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

#### Query Public Keys by publicKey

    curl localhost:8095/api/keyService/v1/pubkey/$PUB_KEY

If no currently valid public keys were found the response is:

    400
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "QueryError",
        "errorMessage": "failed to find public key"
      }
    }

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
        "errorMessage": "failed to find public key"
      }
    }

If the server has problems the response is:

    500
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "ServerError",
        "errorMessage": "failed to find public key"
      }
    }

#### Delete Public Key

Note: example is based on privateKey = "MC8CAQAwCAYDK2VkCgEBBCCHFgxcs/JJ+g94c6tB2MlmML/fZqtkd1r16bUSXXrqGA=="

    curl -XDELETE localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '
    {
      "publicKey": "MC0wCAYDK2VkCgEBAyEArTqgHBLwYqyh3h4GUw4VdK7FX2qxx6b2r0tcmwJ+5pw=", // base64
      "signature": "bK6an5dE9At/WZIuSBSiUqnbid/9XcZAFItfU04q+hCAyWGfIt4dLSf9TGx0/i6eD0Xm9Mb4a8PBCznQvtdRDA==" // Bae64 encoded signature of field _pubKey_
    }'

If the public key was deleted the response is:

    200

In case of an error the response is:

    400
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "DeleteError",
        "errorMessage": "failed to delete public key"
      }
    }

If the server has problems the response is:

    500
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "ServerError",
        "errorMessage": "failed to delete public key"
      }
    }
