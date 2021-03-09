# Risky Venue Configuration Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Risky Venue Configuration: ```https://<FQDN>/distribution/risky-venue-configuration```

## Parameters

- FQDN: One (CDN-) hostname for all distribute APIs
- Authorization NOT required and signatures provided - see [API security](./security.md)
- Payload content-type: application/json

## Scenario
Mobile clients fetch this configuration to be able to calculate how long to show the "book a test" button after visiting a risky venue.

### Response Example (structure)

```json
{
  "durationDays": {
    "optionToBookATest": 11
  }
}
```

For the complete version check [src/static/risky-venue-configuration.json](../../../src/static/risky-venue-configuration.json)
