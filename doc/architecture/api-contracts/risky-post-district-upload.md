# Postal District API: Upload high-risk postal districts

API group: [Upload](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Upload Risky Postal Districts - ```POST https://<FQDN>/upload/high-risk-postal-districts```

### Parameters
- Authorization required and signatures NOT provided - see [API security](./security.md)
- Payload content-type: ```application/json```
- `POST` semantics (idempotent): a previously uploaded list of high-risk postal districts will be completely replaced

## Scenario

The high-risk postal districts are provided by UK public available CV-19 data sources.

The upload service will process the input (see payload example below) and will produce the correct payloads for both distribution versions (see [version 1](risky-post-district-distribution.md) and [version 2](risky-post-district-distribution-v2.md) of the post district distribution API)


### Request Payload Example

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

## Validation

- Non empty post districts keys in the json
- `riskIndicator` ["L","M","H"] corresponding to Low, Medium and High.
- `tierIndicator` For the list of allowed tier indicators are the keys of the [tier-metadata.json](../../../src/static/tier-metadata.json).

## The English Tier System

Tiers 1 through 3 ("EN.Tier1", "EN.Tier2", "EN.Tier3") signify medium, high and very high risk .
Tiers "EN.HighVHigh", "EN.MedHigh", "EN.MedVHigh", "EN.GenericNeutral" are used to signify discrepancy in the status provided for a post district (when a post district is in multiple local authorities and there is a difference in the risk level assigned by each local authority). They do not indicate a specific risk level.

Currently for post districts on the border of England and Wales only the "EN.Border.Tier1" tier is used, supplying a generic message directing the user to nation specific advice. It does not indicate a risk level.

The tiers associated with Wales ("WA.Tier1", "WA.Tier2", "WA.Tier3") signify low, medium and high risk mapping directly to the L/M/H v1 model.

## Response Codes

Default -> [Upload Response Codes](../api-patterns.md#Upload)
