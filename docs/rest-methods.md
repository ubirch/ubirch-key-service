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

All examples are based on the following key pair:

* public key  = MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=
* private key = MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==

*Example with all fields set*
 
* previous public key  = MC0wCAYDK2VkCgEBAyEAgH0cf8WwYiLY/LHtLqhtg7pZaaGI1vNHo4jHDrd6KY0=
* previous prviate key = MC8CAQAwCAYDK2VkCgEBBCC4LZ5r6ueSbFjqM9bUeZKUwWcSyGx2jBs+m5u97adb0g==

```
curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '
{
  "pubKeyInfo": {
    "algorithm": "ECC_ED25519",
    "created": "2018-03-15T21:56:20.819Z",
    "hwDeviceId": "44394342-0d06-4e90-9d91-c2e3bd5612a4",
    "previousPubKeyId": "MC0wCAYDK2VkCgEBAyEAgH0cf8WwYiLY/LHtLqhtg7pZaaGI1vNHo4jHDrd6KY0=",
    "pubKey": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
    "pubKeyId": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
    "validNotAfter": "2018-09-15T21:56:20.819Z",
    "validNotBefore": "2018-03-15T21:56:20.819Z"
  },
  "signature": "2Dx11qm9aEcfY3iqrqRsckjP4SRjp4T3P1L3UTPq1eYeOXXb7MLXzM7SfnGIPuXtqZK60vSKe8MSUmk3fa3jDw==",
  "previousPubKeySignature": "YYAif0Wn25+E7Xl+tH00BiwmvCR8ixi1HPrAxOL+1XgebAtlUIqBK5T0uFcdpWzcie0kURCfuHWJgcscH1w0Bw=="
}'
```

*Example with only mandatory fields*

```
curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '
{
  "pubKeyInfo": {
    "algorithm": "ECC_ED25519",
    "created": "2018-03-15T21:48:32.373Z",
    "hwDeviceId": "39c023be-9d8f-4a72-a05d-271cb928dbc3",
    "pubKey": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
    "pubKeyId": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
    "validNotBefore": "2018-03-15T21:48:32.373Z"
  },
  "signature": "0aMQdrSBeyGbuZefhhLyWRmW3mJPIK+Tp4AtgKIg8eEXUCTogH23NeOfhw3PB1I82Mmsn8yCNC0cyMEFMMwABQ=="
}'
``` 

Valid _algorithm_s are:

* RSA4096
* ECC_ED25519

If successful the response is exactly the key from the request.

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
          "algorithm": "ECC_ED25519",
          "created": "2018-03-15T21:48:32.373Z",
          "hwDeviceId": "39c023be-9d8f-4a72-a05d-271cb928dbc3",
          "pubKey": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
          "pubKeyId": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
          "validNotBefore": "2018-03-15T21:48:32.373Z"
        },
        "signature": "0aMQdrSBeyGbuZefhhLyWRmW3mJPIK+Tp4AtgKIg8eEXUCTogH23NeOfhw3PB1I82Mmsn8yCNC0cyMEFMMwABQ=="
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
          "algorithm": "ECC_ED25519",
          "created": "2018-03-15T21:48:32.373Z",
          "hwDeviceId": "39c023be-9d8f-4a72-a05d-271cb928dbc3",
          "pubKey": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
          "pubKeyId": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
          "validNotBefore": "2018-03-15T21:48:32.373Z"
        },
        "signature": "0aMQdrSBeyGbuZefhhLyWRmW3mJPIK+Tp4AtgKIg8eEXUCTogH23NeOfhw3PB1I82Mmsn8yCNC0cyMEFMMwABQ=="
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

Key pair used for this example:

* public key  = MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=
* private key = MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==

````
curl -XDELETE localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '
{
  "publicKey": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=", // base64
  "signature": "XWBWG1y1HWyVqm3a6pwx21G0kaZcJP/NsSXD7KikLvKDbPT19sCQ8CfWe3YuE3VWReSrUsyA33qRsMV3ioaXBA==" // Bae64 encoded signature of field _pubKey_
}'
````

##### Responses

If the public key was deleted (or didn't exist):

    200

If the signature's invalid:

    400
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "DeleteError",
        "errorMessage": "failed to delete public key"
      }
    }

If the server has a problem:

    500
    {
      "apiVersion": "1.0.0",
      "status": "NOK",
      "error": {
        "errorId": "ServerError",
        "errorMessage": "failed to delete public key"
      }
    }
