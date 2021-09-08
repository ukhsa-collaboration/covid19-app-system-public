# Self Isolation Configuration Distribution

> API Pattern: [Distribution](../../../api-patterns.md#distribution)

## HTTP Request and Response

- Self Isolation Configuration: ```https://<FQDN>/distribution/self-isolation```

### Parameters

- FQDN: One (CDN) hostname for all distributed APIs
- Authorization NOT required and signatures provided - see [API security](../../../api-security.md)
- Payload content-type: `application/json`

## Scenario
Mobile clients fetch this configuration to be able to calculate self isolation duration intervals

### Self Isolation Configuration
#### Response Payload Example (structure)

```json
{
  "durationDays": {
    "indexCaseSinceSelfDiagnosisOnset": 7,
    "indexCaseSinceSelfDiagnosisUnknownOnset": 4,
    "contactCase": 14,
    "maxIsolation": 21,
    "pendingTasksRetentionPeriod": 14,
    "indexCaseSinceTestResultEndDate": 11,
    "testResultPollingTokenRetentionPeriod": 28
  }
}
```

* `indexCaseSinceSelfDiagnosisUnknownOnset` used when the user didn’t enter a start date of the symptoms
* `indexCaseSinceTestResultEndDate` used when the user manually links a positive test result and wasn’t in isolation before

For the complete version check [src/static/self-isolation.json](../../../../../src/static/self-isolation.json)

 
  
