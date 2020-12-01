# Diagnosis Key Submission

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

- Endpoint schema: ```https://<FQDN>/submission/diagnosis-keys```
    - FQDN: Hostname can be different per API
- Authorisation: ```Authorization: Bearer <API KEY>```
    - One API KEY for all mobile phone-facing APIs

## Scenario

Mobile clients send diagnostic keys after the user receives a positive test result and agrees to the upload.

## Payload Example

```json
{
    "diagnosisKeySubmissionToken": "6B162698-ADC5-47AF-8790-71ACF770FFAF" /* see virology-testing-api.md */,
    "temporaryExposureKeys": [
        {
            "key": "base64 KEY1",
            "rollingStartNumber": 12345,
            "rollingPeriod": 144,
            "transmissionRiskLevel": 4,
            "daysSinceOnsetOfSymptoms": 2
        },
        {
            "key": "base64 KEY2",
            "rollingStartNumber": 12489,
            "rollingPeriod": 144,
            "transmissionRiskLevel": 7,
            "daysSinceOnsetOfSymptoms": 6
        },
        {
            "key": "base64 KEYN",
            "rollingStartNumber": 12499,
            "rollingPeriod": 144,
            "transmissionRiskLevel": 6,
            "daysSinceOnsetOfSymptoms": 1
        }
    ]
}
```

### Validation 
- `temporaryExposureKeys` is required array
- `key` required, non-empty base64 encoded string
- `rollingStartNumber` required uint32
- `rollingPeriod` required uint32
- `diagnosisKeySubmissionToken` valid one-time token (associated with a positive Covid19 test result, never used yet)

### Optional properties

The properties `transmissionRiskLevel` and `daysSinceOnsetOfSymptoms` are now supported by the api. In order to preserve backwards compatibility key payloads without these fields will also be accepted.

If `transmissionRiskLevel` is missing from a key the value will be set to 7 by default. 

If `daysSinceOnsetOfSymptoms` is missing from a key on submission, then the field will be omitted from that key in the distribution zip.

### Responses
The API always returns OK (200) even if validation of the payload fails (to prevent api response abuse).
