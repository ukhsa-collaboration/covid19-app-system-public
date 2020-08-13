# Diagnosis Key Submission

Version V3.0, 2020-08-08

API group: [Submission](../api-patterns.md#Submission)

- Endpoint schema: ```https://<FQDN>/submission/diagnosis-keys``` 
    - FQDN: Hostname can be different per API
- Authorisation: ```Authorization: Bearer <API KEY>```
    - One API KEY for all mobile phone-facing APIs

## Payload Example

```json
{
    "diagnosisKeySubmissionToken": "6B162698-ADC5-47AF-8790-71ACF770FFAF" /* see virology-testing-api.md */,
    "temporaryExposureKeys": [
        {
            "key": "base64 KEY1",
            "rollingStartNumber": 12345,
            "rollingPeriod": 144
        },
        {
            "key": "base64 KEY2",
            "rollingStartNumber": 12489,
            "rollingPeriod": 144
        },
        {
            "key": "base64 KEYN",
            "rollingStartNumber": 12499,
            "rollingPeriod": 144
        }
    ]
}
```

### Validation 
- `temporaryExposureKeys` is required array
- `key` required, non-empty base64 encoded string
- `rollingStartNumber` required uint32
- `rollingPeriod` required uint32 (only value 144 is a valid)
- `diagnosisKeySubmissionToken` valid one-time token (associated with a positive Covid19 test result, never used yet)

### Defaults 
- After receiving and validating the payload from the client, the backend also defaults the `transmissionRisk` to 7 for each key in the `temporaryExposureKeys` array.

### Responses
The API always returns OK (200) even if validation of the payload fails (to prevent api response abuse).
