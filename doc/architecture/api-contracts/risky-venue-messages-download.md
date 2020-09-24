# Risky Venue Messages Configuration-Download

API group: [Download](FIXME - similar to ../api-patterns.md#Distribution, but ext-system-facing)

- Endpoint schema: ```https://<FQDN>/download/risky-venue-messages-configuration```
  - FQDN: One (CDN-) hostname for all download APIs
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Scenario
- Rush website downloads the list of risky venue message configurations periodically (i.e. between hourly and daily) and caches the configuration locally
- User of rush website can chose one of the offered message types (and must provide a parameter value, if supported by the selected message type)

## Payload Example

```json
{
    "riskyVenueAlertMessages": [
        {
            "messageType": "M1",
            "messageDescription": "(placeholder / sample) Inform user about check-in at a risky venue.",
            "additionalParameterRequired": false
        },
        {
            "messageType": "M2",
            "messageDescription": "(placeholder / sample) Inform user about check-in at a risky venue and instruct him/her to self-isolate.",
            "additionalParameterRequired": false
        },
        {
            "messageType": "M3",
            "messageDescription": "(placeholder / sample) Inform user about check-in at a risky venue and instruct him/her to call the provided phone number",
            "additionalParameterRequired": true,
            "parameterType": "PHONE_NUMBER",
            "parameterDescription": "Phone number"
        }
    ]
}
```

### Parameter Types supported in V3.5+

- `PHONE_NUMBER`: a valid UK phone number (e.g. "07911 123456")
