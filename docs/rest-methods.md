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
* previous private key = MC8CAQAwCAYDK2VkCgEBBCC4LZ5r6ueSbFjqM9bUeZKUwWcSyGx2jBs+m5u97adb0g==

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

#### Create (MessagePack)

All examples are based on the following key pair:

* public key  = ???
* private key = ???

*Example with all fields set*
 
* previous public key  = ???
* previous private key = ???

```
curl -XPOST localhost:8095/api/keyService/v1/pubkey/mpack -H "Content-Type: application/octet-stream" -d '10010101110011010000000000010010101100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000011100110110010100110101011110000000001100001101010100101100001011011000110011101101111011100100110100101110100011010000110110110101011010001010100001101000011010111110100010101000100001100100011010100110101001100010011100110100111011000110111001001100101011000010111010001100101011001000000111010101010011010000111011101000100011001010111011001101001011000110110010101001001011001001011000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000111001101100101001101010111100101001100111000001110101011000100100101101100101011110011101101000000000001000000101010110111101111011001010110101000001111101011111100100001111011100101101010001100100110000000011010011101011000011100010101100001110111010110100001011001010010111010110010001111101010000011001110010000001110101101001001100101110100010101110011111000000101011010111011001100001011011000110100101100100010011100110111101110100010000010110011001110100011001010111001011001110000000011110000100110011100011011010111001110110011000010110110001101001011001000100111001101111011101000100001001100101011001100110111101110010011001010000111011011010000000000100000010001111010011110101010000101110010010011001011011101100110010110110011011001000000100100011101101011100011011111111100010010011011000101111010101010111011100010001110010001111111011110111111000101100010110101110101101110000110100011101100001010000101001010111001010011000100000101001101001011000011011111001011110000100000110110100101001110010101100101101000010000001100101000101000001100000000011011001101101001110000010110010010010100011011001111101001011111110011100110000011001110101100101010001110000000010'
```

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
  "signature": "XWBWG1y1HWyVqm3a6pwx21G0kaZcJP/NsSXD7KikLvKDbPT19sCQ8CfWe3YuE3VWReSrUsyA33qRsMV3ioaXBA==" // Bae64 encoded signature of field _publicKey_
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
