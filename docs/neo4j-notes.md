# Query Trust Relationships

```MATCH p=()-[r:TRUST]->() RETURN p LIMIT 25```

# Query Web-of-Trust

```
MATCH (source:PublicKey {infoPubKey: 'MC0wCAYDK2VkCgEBAyEAfijeTNLiYDSJoG2KEBZfsUroaUCqxKur3iJAO/aU9x0='})
  MATCH p=(source)-[:TRUST*1..5]->(target:PublicKey)
  WHERE
    all(trust IN relationships(p) WHERE trust.trustLevel >= 50 AND trust.trustNotValidAfter >= "2018-09-26T13:06:36.600Z")
    AND source <> target
  RETURN p
```
