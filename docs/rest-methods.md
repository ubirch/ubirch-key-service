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

This method is idempotent. Hence uploading an existing key does not produce an error but instead behaves as if the
upload had been successful.

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

// TODO provide a working curl call since none of these is working
```
curl -XPOST localhost:8095/api/keyService/v1/pubkey/mpack -H "Content-Type: application/octet-stream" -d '9512b0000000000000000000000000000000000187a9616c676f726974686dab4543435f45443235353139a763726561746564ce5b7426abaa68774465766963654964b000000000000000000000000000000000a67075624b6579da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caa87075624b65794964da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caad76616c69644e6f744166746572ce5d555a2bae76616c69644e6f744265666f7265ce5b7426abda00409eca7dd20739063735aca17bd0ed4c4b8c2cfd12628e680f2a7b3f68fdeda33d5f2773f62e8182924c3b89ac38202e5d019d8b04f5cc82d262fa0a4100d45f05'
curl -XPOST localhost:8095/api/keyService/v1/pubkey/mpack -H "Content-Type: application/octet-stream" -d '9512b0000000000000000000000000000000000187a9616c676f726974686dab4543435f45443235353139a763726561746564ce5b7426abaa68774465766963654964b000000000000000000000000000000000a67075624b6579da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caa87075624b65794964da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caad76616c69644e6f744166746572ce5d555a2bae76616c69644e6f744265666f7265ce5b7426abda00409eca7dd20739063735aca17bd0ed4c4b8c2cfd12628e680f2a7b3f68fdeda33d5f2773f62e8182924c3b89ac38202e5d019d8b04f5cc82d262fa0a4100d45f05'
curl -XPOST localhost:8095/api/keyService/v1/pubkey/mpack -H "Content-Type: application/octet-stream" --data-binary '9512b0000000000000000000000000000000000187a9616c676f726974686dab4543435f45443235353139a763726561746564ce5b7426abaa68774465766963654964b000000000000000000000000000000000a67075624b6579da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caa87075624b65794964da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caad76616c69644e6f744166746572ce5d555a2bae76616c69644e6f744265666f7265ce5b7426abda00409eca7dd20739063735aca17bd0ed4c4b8c2cfd12628e680f2a7b3f68fdeda33d5f2773f62e8182924c3b89ac38202e5d019d8b04f5cc82d262fa0a4100d45f05'
curl -XPOST localhost:8095/api/keyService/v1/pubkey/mpack --data-binary '9512b0000000000000000000000000000000000187a9616c676f726974686dab4543435f45443235353139a763726561746564ce5b7426abaa68774465766963654964b000000000000000000000000000000000a67075624b6579da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caa87075624b65794964da00209c9f9ffd51bd6f77bebc2e3e0b72457ffdb24b23ebf889a4f91d6008040af0caad76616c69644e6f744166746572ce5d555a2bae76616c69644e6f744265666f7265ce5b7426abda00409eca7dd20739063735aca17bd0ed4c4b8c2cfd12628e680f2a7b3f68fdeda33d5f2773f62e8182924c3b89ac38202e5d019d8b04f5cc82d262fa0a4100d45f05'
curl -XPOST localhost:8095/api/keyService/v1/pubkey/mpack --data-binary '100101010001001010110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000110000111101010010110000101101100011001110110111101110010011010010111010001101000011011011010101101000101010000110100001101011111010001010100010000110010001101010011010100110001001110011010011101100011011100100110010101100001011101000110010101100100110011100101101101110100001001101010101110101010011010000111011101000100011001010111011001101001011000110110010101001001011001001011000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000101001100111000001110101011000100100101101100101011110011101101000000000001000001001110010011111100111111111110101010001101111010110111101110111101111101011110000101110001111100000101101110010010001010111111111111101101100100100101100100011111010111111100010001001101001001111100100011101011000000000100000000100000010101111000011001010101010000111000001110101011000100100101101100101011110010100100101100100110110100000000000100000100111001001111110011111111111010101000110111101011011110111011110111110101111000010111000111110000010110111001001000101011111111111110110110010010010110010001111101011111110001000100110100100111110010001110101100000000010000000010000001010111100001100101010101101011101100110000101101100011010010110010001001110011011110111010001000001011001100111010001100101011100101100111001011101010101010101101000101011101011100111011001100001011011000110100101100100010011100110111101110100010000100110010101100110011011110111001001100101110011100101101101110100001001101010101111011010000000000100000010011110110010100111110111010010000001110011100100000110001101110011010110101100101000010111101111010000111011010100110001001011100011000010110011111101000100100110001010001110011010000000111100101010011110110011111101101000111111011110110110100011001111010101111100100111011100111111011000101110100000011000001010010010010011000011101110001001101011000011100000100000001011100101110100000001100111011000101100000100111101011100110010000010110100100110001011111010000010100100000100000000110101000101111100000101'
```

#### Trust Keys

This method is idempotent. Hence trusting a key that already has the callers trust is successful.

##### Curl Example

All examples are based on the following key pairs:

###### Caller (Key A)

* public key  = MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=
* private key = MC8CAQAwCAYDK2VkCgEBBCBaVXkOGCrGJrrQcfFSOVXTDKJRN5EvFs+UwHVSBIrK6Q==

###### Key To Trust (Key B)

* public key  = MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=
* private key = MC8CAQAwCAYDK2VkCgEBBCCnZ7tKYA/dzNPqgRRe6yBb+q7cj0AvWA6FVf6nxOtGlg==

###### Uploading Both Keys

```
# Key A
curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '{"pubKeyInfo":{"algorithm":"ECC_ED25519","created":"2018-09-10T11:37:56.094Z","hwDeviceId":"a7f0023c-59b1-463a-8377-bc575c2d6ec6","pubKey":"MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=","pubKeyId":"MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=","validNotBefore":"2018-09-10T12:36:56.159Z"},"signature":"Yd85sqbcN6m1lPQbGtLjEfiBTSZqnW1lrLCgPFQhSsZtq8qQ17w6gzh2Azt+B5exhD+0RUnKKaYGnKy7DyzvBg=="}'
# Key B
curl -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '{"pubKeyInfo":{"algorithm":"ECC_ED25519","created":"2018-09-10T11:37:56.535Z","hwDeviceId":"08c378c3-086a-4e8e-93b9-a416e125e251","pubKey":"MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=","pubKeyId":"MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=","validNotBefore":"2018-09-10T12:36:56.535Z"},"signature":"rbtYmRQ93a8SQ72rxn5zC10FqAa9o2QDqtg13CYVJ0+Z/QC/bz4qPgKeIbdrTgh+MmAhClUHlscc/Kw64TeBAA=="}'
```

###### Trust In Both Directions

```
# trust(A --> B)
curl -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '{
  "signature": "yWMI1pe6Hi7Wf4UxUb1NOUHNBLu/ZyjpDVIqG6mK6mveKoqbU5K1z7cizthAUVVg8HLWRR3uIYLUV/Rx+06ADw==",
  "trustRelation": {
    "created": "2018-09-10T12:37:56.544Z",
    "sourcePublicKey": "MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=",
    "targetPublicKey": "MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=",
    "trustLevel":10,
    "validNotAfter":"2018-12-10T12:37:56.544Z"
  }
}'

# trust(B --> a)
curl -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '{"signature":"REbdGZ2gkXlRopNXz0p7w9r+fWxYNyvNp7eCgTKLBb48hRLhgUT/ur/dHrr8/CKc0pELL0Ne1jawKOU0SINZBw==","trustRelation":{"created":"2018-09-10T12:37:56.558Z","sourcePublicKey":"MC0wCAYDK2VkCgEBAyEAV4aTMZNuV2bLEy/VwZQTpxbPEVZ127gs88TChgjuq4s=","targetPublicKey":"MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=","trustLevel":10,"validNotAfter":"2018-12-10T12:37:56.558Z"}}'
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
