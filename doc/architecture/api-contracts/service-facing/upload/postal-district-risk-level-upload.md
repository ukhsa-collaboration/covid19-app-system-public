# Postal District Risk Level Upload

> API Pattern: [Upload](../../../api-patterns.md#upload)

## HTTP Request and Response

- Upload Postal Districts Risk Levels - ```POST https://<FQDN>/upload/high-risk-postal-districts```

### Parameters
- Authorization required and signatures NOT provided - see [API security](../../../api-security.md)
- Payload content-type: ```application/json```
- `POST` semantics (idempotent): a previously uploaded list of high-risk postal districts will be completely replaced

## Scenario

The postal district risk levels are provided Joint Biosecurity Centre staff.

The upload service processes the input (see payload example below) and produces the correct payloads for both distribution versions (see [post district distribution API](../../mobile-facing/distribution/postal-district-risk-level-distribution.md))

### Risky Postal District
#### Request Payload Example

```json
{
  "postDistricts": {
    "M1": {
      "riskIndicator": "M",
      "tierIndicator": "EN.Tier1"
    },
    "H1": {
      "riskIndicator": "H",
      "tierIndicator": "EN.Tier2"
    }
  },
  "localAuthorities": {
    "E02000100": {
      "tierIndicator": "EN.Tier1"
    },
    "E02000101": {
      "tierIndicator": "EN.Tier2"
    }
  }
}
```

### Validation

- Non empty post districts keys in the json
- `riskIndicator` ["L","M","H"] corresponding to Low, Medium and High.
- `tierIndicator` For the list of allowed tier indicators are the keys of the [tier-metadata.json](../../../../../src/static/tier-metadata.json).

### The Tier System

Tiers 1 through 3 ("EN.Tier1", "EN.Tier2", "EN.Tier3") signify medium, high and very high risk .
Tiers "EN.HighVHigh", "EN.MedHigh", "EN.MedVHigh", "EN.GenericNeutral" are used to signify discrepancy in the status provided for a post district (when a post district is in multiple local authorities and there is a difference in the risk level assigned by each local authority). They do not indicate a specific risk level.

Currently for post districts on the border of England and Wales only the "EN.Border.Tier1" tier is used, supplying a generic message directing the user to nation specific advice. It does not indicate a risk level.

The tiers associated with Wales ("WA.Tier1", "WA.Tier2", "WA.Tier3") signify low, medium and high risk mapping directly to the L/M/H v1 model.

> Note: The tiers, and associated policy statement,  have needed to change over time, resulting in the need for a tiers metadata dataset (JSON) that is maintained separately and uploaded to S3 by a release pipeline.
> The tiers metadata is incorporated by the upload lambda when generating the resulting, combined, output for distribution.
### HTTP Response Codes

- `200 OK` ok
- `422 Unprocessable Entity` invalid json request body
- `500 Internal Server Error` internal server error
- `503 Service Unavailable` maintenance mode

#### Maintenance Mode

When the service is in [maintenance mode](../../../../design/details/api-maintenance-mode.md), all services will return `503 Service Unavailable`. Maintenance mode is used when performing operations like backup and restore of the service data.
