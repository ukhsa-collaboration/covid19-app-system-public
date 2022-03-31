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
    "indexCaseSinceSelfDiagnosisOnset": 11,
    "indexCaseSinceSelfDiagnosisUnknownOnset": 9,
    "contactCase": 11,
    "maxIsolation": 21,
    "pendingTasksRetentionPeriod": 14,
    "indexCaseSinceTestResultEndDate": 11,
    "testResultPollingTokenRetentionPeriod": 28
  },
  "england": {
    "indexCaseSinceSelfDiagnosisOnset": 11,
    "indexCaseSinceSelfDiagnosisUnknownOnset": 9,
    "contactCase": 11,
    "maxIsolation": 21,
    "pendingTasksRetentionPeriod": 14,
    "indexCaseSinceTestResultEndDate": 11,
    "testResultPollingTokenRetentionPeriod": 28
  },
  "wales": {
    "indexCaseSinceSelfDiagnosisOnset": 6,
    "indexCaseSinceSelfDiagnosisUnknownOnset": 4,
    "contactCase": 11,
    "maxIsolation": 16,
    "pendingTasksRetentionPeriod": 14,
    "indexCaseSinceTestResultEndDate": 6,
    "testResultPollingTokenRetentionPeriod": 28
  }
}
```

- `indexCaseSinceSelfDiagnosisUnknownOnset` used when the user didn’t enter a start date of the symptoms
- `indexCaseSinceTestResultEndDate` used when the user manually links a positive test result and wasn’t in isolation before
- `england` and `wales` are used to provide country-specific configurations. The names doesn't mention `durationDays` since there are plans to introduce new non-integer fields in the future
- `durationDays` is shared configuration for England and Wales and is used by older app versions (<=4.6.1)  

For the complete version check [src/static/self-isolation.json](../../../../../src/static/self-isolation.json)
