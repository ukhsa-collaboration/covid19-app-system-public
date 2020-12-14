# Risky Post Districts Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Risky Post Districts: ```GET https://<FQDN>/distribution/risky-post-districts```

### Parameters
- FQDN: One (CDN-) hostname for all distribute APIs
- Authorization NOT required and signatures provided - see [API security](./security.md)
- Payload content-type: application/json

## Response Payload Example

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
