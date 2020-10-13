# Risky Post Districts Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

- Endpoint schema: ```https://<FQDN>/distribution/risky-post-districts-v2```
- FQDN: One (CDN-) hostname for all distribute APIs
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Payload Example

```json
{
  "postDistricts": {
    "<post district code 1>": "<risk indicator 1>",
    "<post district code 2>": "<risk indicator 2>",
    "<post district code 3>": "<risk indicator 3>",
    …
  },
  "riskLevels": {
    "<risk indicator 1>": {
      "colorScheme": "yellow",
      "name": {
        "en": "Tier 2",
        …
      },
      "heading": {
        "en": "Data from NHS shows…",
        …
      },
      "content": {
        "en": "Your local authority…",
        …
      },
      "linkTitle": {
        "en": "Restrictions in your area",
        …
      },
      "linkUrl": {
        "en": "https://gov.uk/somewhere",
        …
      }
    },
    …
  }
}
```

## Validation

- All fields are required.
- Acceptable values for the `<risk indicator>` are defined by the API itself. All `<risk indicator>` values used must provide their definition as part of the `riskLevelIndicators` object.
- Acceptable values for `colorScheme` are `green`, `yellow`, `red`, `amber`, `neutral`.
- `heading`, `content`, `name`, `linkTitle`, and `linkUrl` are localised.
- `heading` and `content` may contain multiple paragraphs of text; they may also be left empty (but must not be null)
- `name` should be short. The clients may use this as part of a sentence.
- `linkTitle` should be a few words. The clients may use this as part of a button.

## Notes

- [Post Districts](https://en.wikipedia.org/wiki/List_of_postcode_districts_in_the_United_Kingdom)
