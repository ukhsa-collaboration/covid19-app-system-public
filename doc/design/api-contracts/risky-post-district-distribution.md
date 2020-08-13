# Risky Post Districts Distribution

Version V3.0, 2020-08-08

API group: [Distribution](../api-patterns.md#Distribution)

- Endpoint schema: ```https://<FQDN>/distribution/risky-post-districts```
- FQDN: One (CDN-) hostname for all distribute APIs
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Payload Example

```json
{
  "postDistricts": {
    "<post district code 1>": "<risk indicator 1>",
    "<post district code 2>": "<risk indicator 2>",
    "<post district code 3>": "<risk indicator 3>"
  }
}
```

## Validation

- Acceptable values for the `<risk indicator>` are ["L","M","H"] corresponding to Low, Medium and High.
- The payload contains the full list of post districts.

## Notes

- [Post Districts](https://en.wikipedia.org/wiki/List_of_postcode_districts_in_the_United_Kingdom)
