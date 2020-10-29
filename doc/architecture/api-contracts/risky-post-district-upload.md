# Postal District API: Upload high-risk postal districts

API group: [Upload](../guidebook.md#system-apis-and-interfaces)

The high-risk postal districts are provided by UK public available CV-19 data sources.

The risk levels per post districts are mapped in two systems. The upload service can handle two formats and will produce the correct payloads for both distribution versions (see [version 1](risky-post-district-distribution.md) and [version 2](risky-post-district-distribution-v2.md) of the post district distribution API)

## Endpoints

### Version 1

The version 1 system is a risk indicator of "H","M" or "L" (for high, medium and low).

- Upload a csv file with a list of high-risk postal districts

```bash
POST https://<FQDN>/upload/high-risk-postal-districts
```

- `POST` semantics (idempotent): a previously uploaded list of high-risk postal districts will be completely replaced
- Payload content-type: ```text/csv```

### Version 2

The version 2 system assigns tiers for higher granularity and can be used for post districts that have no assigned tier indicator. The source data requires the presence of the version 1 information as it is the fallback value used if the tier information is not available.

- Upload a csv file with a list of high-risk postal districts

```bash
POST https://<FQDN>/upload/high-risk-postal-districts
```

- `POST` semantics (idempotent): a previously uploaded list of high-risk postal districts will be completely replaced
- Payload content-type: ```text/csv```

### Version 1 Example

```csv
# postal_district_code, risk_indicator
"CODE1", "H"
"CODE2", "M"
"CODE3", "L"
```

### Version 2 Example

```csv
# postal_district_code, risk_indicator, tier_indicator
"CODE1", "H","EN.Tier1"
"CODE2", "M","EN.Tier3"
"CODE3", "L",""
```

An empty tier_indicator value will drop the entry from the version 2 distribution payload.
An empty value is only accepted as "". The service will return an error for malformed entries like:

```csv
"CODE3BAD", "L",
"CODEMISSING", "H"
```

## Validation

- `postal_district_code` valid UK postal district code
- `risk_indicator` ["L","M","H"] corresponding to Low, Medium and High.
- `tier_indicator` ["EN.Tier1", "EN.Tier2", "EN.Tier3","EN.HighVHigh", "EN.MedHigh", "EN.MedVHigh", "EN.GenericNeutral", "EN.Border.Tier1", "EN.Border.Tier2", "EN.Border.Tier3", "WA.Tier1", "WA.Tier2", "WA.Tier3", "WA.Border.Tier1", "WA.Border.Tier2", "WA.Border.Tier3", ""].

## The English Tier System

Tiers 1 through 3 ("EN.Tier1", "EN.Tier2", "EN.Tier3") signify medium, high and very high risk .
Tiers "EN.HighVHigh", "EN.MedHigh", "EN.MedVHigh", "EN.GenericNeutral" are used to signify discrepancy in the status provided for a post district (when a post district is in multiple local authorities and there is a difference in the risk level assigned by each local authority). They do not indicate a specific risk level.

Currently for post districts on the border of England and Wales only the "EN.Border.Tier1" tier is used, supplying a generic message directing the user to nation specific advice. It does not indicate a risk level.

The tiers associated with Wales ("WA.Tier1", "WA.Tier2", "WA.Tier3") signify low, medium and high risk mapping directly to the L/M/H v1 model.

## Response Codes

Default -> [Upload Response Codes](../api-patterns.md#Upload)
