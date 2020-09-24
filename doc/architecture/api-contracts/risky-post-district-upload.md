# Postal District API: Upload high-risk postal districts

API group: [Upload](../ag-architecture-guidebook#System-APIs-and-Interfaces)

The high-risk postal districts are provided by UK public available CV-19 data sources.

## Endpoints

- Upload a csv file with a list of high-risk postal districts 
```
POST https://<FQDN>/upload/high-risk-postal-districts
```
- `POST` semantics (idempotent): a previously uploaded list of high-risk postal districts will be completely replaced
- Payload content-type: ```text/csv```

## CSV File Example

```csv
# postal_district_code, risk_indicator
"CODE1", "H"
"CODE2", "M"
"CODE3", "L"
```

## Validation

- `postal_district_code` valid UK postal district code
- `risk_indicator` ["L","M","H"] corresponding to Low, Medium and High.

## Response Codes

Default -> [Upload Response Codes](../api-patterns.md#Upload)
