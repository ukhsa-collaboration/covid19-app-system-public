# Risky Venue Configuration Distribution

> API Pattern: [Distribution](../../../api-patterns.md#distribution)

## HTTP Request and Response

- Risky Venue Configuration: ```https://<FQDN>/distribution/risky-venue-configuration```

### Parameters

- FQDN: One (CDN-) hostname for all distribute APIs
- Authorization NOT required and signatures provided - see [API security](../../../api-security.md)
- Payload content-type: application/json

## Scenario
Mobile clients fetch this configuration to be able to calculate how long to show the "book a test" button after visiting a risky venue.

### Risky Venue Configuration
#### Response Payload Example (structure)

```json
{
  "durationDays": {
    "optionToBookATest": 11
  }
}
```

For the complete version check [src/static/risky-venue-configuration.json](../../../../../src/static/risky-venue-configuration.json)
