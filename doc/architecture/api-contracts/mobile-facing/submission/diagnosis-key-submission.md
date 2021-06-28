# Diagnosis Key Submission

> API Pattern: [Submission](../../../api-patterns.md#submission)

## HTTP Request and Response

- Submit Diagnosis Keys - ```POST https://<FQDN>/submission/diagnosis-keys```

### Parameters
- FQDN: Hostname can be different per API
- Authorization required and signatures provided - see [API security](../../../api-security.md)

## Scenario

Mobile clients send diagnostic keys after the user receives a positive test result and agrees to the upload.

### Submit Diagnosis Keys

#### Request Payload Example

```json
{
    "diagnosisKeySubmissionToken": "6B162698-ADC5-47AF-8790-71ACF770FFAF",
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

#### Required properties 
- `diagnosisKeySubmissionToken` valid one-time token (associated with a positive COVID-19 test result that has never been used yet). Token must be obtained via the [virology testing api](virology-test-order-submission.md).
- `temporaryExposureKeys` is required array
- `key` required, non-empty base64 encoded string
- `rollingStartNumber` required uint32
- `rollingPeriod` required uint32

#### Optional properties

The properties `transmissionRiskLevel` and `daysSinceOnsetOfSymptoms` are now supported by the api. In order to preserve backwards compatibility key payloads without these fields will also be accepted.

If `transmissionRiskLevel` is missing from a key the value will be set to 7 by default. 

If `daysSinceOnsetOfSymptoms` is missing from a key on submission, then the field will be omitted from that key in the distribution zip.

### HTTP Response Codes
The API always returns `200 OK` even if validation of the payload fails (to prevent api response abuse).

### Notes
- See [Key User Journeys](../../../journeys.md) for conceptual system flows
