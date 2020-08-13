# Symptoms Questionnaire Distribution

Version V3.0, 2020-08-08

API group: [Distribution](../api-patterns.md#Distribution)

- Endpoint schema: ```https://<FQDN>/distribution/symptomatic-questionnaire```
  - FQDN: One (CDN-) hostname for all distribute APIs
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Scenario
- Client downloads the questionnaire and renders `title` and `description` of each item in the symptoms array
- Client uses the `riskWeight` to calculate the total risk of all the checked symptoms
- Client uses the `riskThreshold` to check if the total risk is above or below this value and progress to the next screen accordingly
- Client uses the `symptomsOnsetWindowDays` for the selection of the date when the symptoms have started
 
## Payload Example

```json
{
  "symptoms": [
    {
      "title": {
        "en-GB": "A high temperature (fever)"
      },
      "description": {
        "en-GB": "This means that you feel hot to touch on your chest or back (you do not need to measure your temperature)."
      },
      "riskWeight": 1
    },
    {
      "title": {
        "en-GB": "A new continuous cough"
      },
      "description": {
        "en-GB": "This means coughing a lot for more than an hour, or 3 or more coughing episodes in 24 hours (if you usually have a cough, it may be worse than usual)."
      },
      "riskWeight": 0
    }
  ],
  "riskThreshold": 0.5, 
  "symptomsOnsetWindowDays": 14
}
```

For the complete questionnaire version check [src/static/symptomatic-questionnaire.json](../../../src/static/symptomatic-questionnaire.json)
