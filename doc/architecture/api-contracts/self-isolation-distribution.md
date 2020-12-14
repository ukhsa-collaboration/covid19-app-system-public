# Self Isolation Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Self Isolation Configuration: ```https://<FQDN>/distribution/self-isolation```

## Parameters

- FQDN: One (CDN-) hostname for all distribute APIs
- Authorization NOT required and signatures provided - see [API security](./security.md)
- Payload content-type: application/json

## Scenario
Mobile clients fetch this configuration to be able to calculate self isolation duration intervals

### Response Example (structure)

```json
{
  "durationDays": {
    "indexCaseSinceSelfDiagnosisOnset": 7,
    "indexCaseSinceSelfDiagnosisUnknownOnset": 4,
    "contactCase": 14,
    "maxIsolation": 21,
    "pendingTasksRetentionPeriod": 14
  }
}
```

For the complete version check [src/static/self-isolation.json](../../../src/static/self-isolation.json)

#### Note
`indexCaseSinceSelfDiagnosisUnknownOnset` used when the user didn’t enter a start date of the symptoms

 
 
  
