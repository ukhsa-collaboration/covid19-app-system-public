# Self Isolation Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

- Endpoint schema: ```https://<FQDN>/distribution/self-isolation```
  - FQDN: One (CDN-) hostname for all distribute APIs
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Scenario
Mobile clients fetch this configuration to be able to calculate self isolation duration intervals

## Payload Example

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

 
 
  
