# Symptoms Submission (Out of scope for V3/F&F release)

Version V3.0, 2020-08-08

API group: [Submission](../api-patterns.md#Submission)

- Endpoint schema: ```https://<FQDN>/submission/self-diagnosis```
  - FQDN: Hostname can be different per API
- Authorization: ```Authorization: Bearer <API KEY>```
  - One API KEY for all mobile phone-facing APIs

## Payload Example

```json
{
    "questionaire_answers": [
        {
            "title": {
                "en-GB": "A high temperature (fever)"
            },
            "answer": true|false
        }
    ],
    "recommendation":"self-isolation"|"all-clear"
}
```
