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
"CODE1", "H","Tier 1"
"CODE2", "M","Tier 3"
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
- `tier_indicator` ["Tier 1","Tier 2","Tier 3", ""].

## Response Codes

Default -> [Upload Response Codes](../api-patterns.md#Upload)
