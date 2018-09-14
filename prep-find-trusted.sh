#!/usr/bin/env bash
###### upload public keys
# Key A
curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '{"pubKeyInfo":{"algorithm":"ECC_ED25519","created":"2018-09-14T14:36:08.536Z","hwDeviceId":"93054176-632f-4298-98d4-3436e7719011","pubKey":"MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=","pubKeyId":"MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=","validNotBefore":"2018-09-14T15:35:08.588Z"},"signature":"C3mLLb2HBnfW/6mQFtsUmTYKGh7DmxR3WDmkl7xGbrUP2IVredARTWhguGy9lPeroo8Qd0La8+hLtj4YHmVIAQ=="}'
# Key B
curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '{"pubKeyInfo":{"algorithm":"ECC_ED25519","created":"2018-09-14T14:36:08.921Z","hwDeviceId":"32662320-67ec-4684-a58c-bfd7fc3cb2bc","pubKey":"MC0wCAYDK2VkCgEBAyEAZ68y5f3zwInZVWg2q4eBdfbSzM0UK5l1xroDQpQBF4Y=","pubKeyId":"MC0wCAYDK2VkCgEBAyEAZ68y5f3zwInZVWg2q4eBdfbSzM0UK5l1xroDQpQBF4Y=","validNotBefore":"2018-09-14T15:35:08.921Z"},"signature":"iH/qSxdu8NrnTgBxFYO4jfXvn7no4fNYUsk/5xvzsyvVd06kJxDj1yS1qQ8e+8UN8ec9776jlEO0LKRGP8y0Dw=="}'
# Key C
curl -i -XPOST localhost:8095/api/keyService/v1/pubkey -H "Content-Type: application/json" -d '{"pubKeyInfo":{"algorithm":"ECC_ED25519","created":"2018-09-14T14:36:08.925Z","hwDeviceId":"f2c467a6-ea37-4d2f-b0c4-132c75c34a69","pubKey":"MC0wCAYDK2VkCgEBAyEAZMPpszVmofwoREiZ07buzXGKx2rdHb4I4yCrO4/7sOI=","pubKeyId":"MC0wCAYDK2VkCgEBAyEAZMPpszVmofwoREiZ07buzXGKx2rdHb4I4yCrO4/7sOI=","validNotBefore":"2018-09-14T15:35:08.926Z"},"signature":"nEOlMc9STR+vmLAPknYdvGGc7gx17N39lMERkEF6EmgIlOojlMjxodloYv0AmZLvF+8JTpVROFSA8zM7BVf6AQ=="}'

###### trust keys
# trust(A --trustLevel:50--> B)
curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '{"trustRelation":{"created":"2018-09-14T15:36:08.936Z","sourcePublicKey":"MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=","targetPublicKey":"MC0wCAYDK2VkCgEBAyEAZ68y5f3zwInZVWg2q4eBdfbSzM0UK5l1xroDQpQBF4Y=","trustLevel":50,"validNotAfter":"2018-12-14T15:36:08.936Z"},"signature":"wvTg7KSWLuVG/tycst61EglA1W2KQmvMwH3J346V293H7T4MK6NGydKtijtR7LbftnopKetzL8ZdfWm2Oc/wCg=="}'
# trust(A --trustLevel:70--> C)
curl -i -XPOST localhost:8095/api/keyService/v1/pubkey/trust -H "Content-Type: application/json" -d '{"trustRelation":{"created":"2018-09-14T15:36:08.952Z","sourcePublicKey":"MC0wCAYDK2VkCgEBAyEA+alWF5nfiw7RYbRqH5lAcFLjc13zv63FpG7G2OF33O4=","targetPublicKey":"MC0wCAYDK2VkCgEBAyEAZMPpszVmofwoREiZ07buzXGKx2rdHb4I4yCrO4/7sOI=","trustLevel":70,"validNotAfter":"2018-12-14T15:36:08.952Z"},"signature":"1N+54Ix/O6/967OHWz8Cgcx7b40B9gQhXS5qZqvA9KDYijEOtCibXyGpmImBY66FVbSAsdWNFuBgB4fJzdCBCA=="}'
