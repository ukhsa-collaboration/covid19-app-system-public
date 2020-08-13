# Risky Venue Distribution

Version V3.0, 2020-08-08

API group: [Distribution](../api-patterns.md#Distribution)

- Endpoint schema: ```https://<FQDN>/distribution/risky-venues```
  - FQDN: One (CDN-) hostname for all distribute APIs
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Scenario
- Client downloads the list of risky venues periodically
- Later client scans some venue qr code, extracts the venue id from the base64 encoded string (in the qr code payload) and compares it (plus the risky window) with the risky venues it has stored locally and reacts based on the result of this comparison 

## Payload Example

```json
{
    "venues" : [
        {
            "id": "4WT59M5Y",
            "riskyWindow": {
              "from": "2019-07-04T13:33:03Z",
              "until": "2019-07-04T23:59:03Z"
            }
        }
    ]
}
```

For the complete questionnaire version check [src/static/risky-venues.json](../../../src/static/risky-venues.json)

#### Notes
- The server sends the date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
- Example: if the downstream data source has a granularity of a day, the time will be default to 00:00:00
