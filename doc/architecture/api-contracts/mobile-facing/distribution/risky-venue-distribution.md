# Risky Venue Distribution

API group: [Distribution](../../../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Risky Venues: ```GET https://<FQDN>/distribution/risky-venues```

### Parameters

- FQDN: One (CDN) hostname for all distributed APIs
- Authorization NOT required and signatures provided - see [API security](../../security.md)
- Payload content-type: `application/json`

## Scenario
- Client downloads the list of risky venues periodically
- Later client scans some venue qr code, extracts the venue id from the base64 encoded string (in the qr code payload) and compares it (plus the risky window) with the risky venues it has stored locally and reacts based on the result of this comparison 

## Example: Risky Venues

### Response Example: Message Type "M1" (without optional parameter):
```json
{
    "venues" : [
        {
            "id": "4WT59M5Y",
            "riskyWindow": {
              "from": "2019-07-04T13:33:03Z",
              "until": "2019-07-04T23:59:03Z"
            },
            "messageType": "M1"
        }
    ]
}
```

### Response Example: Message Type "M2" (without optional parameter):
```json
{
    "venues" : [
        {
            "id": "4WT59M5Y",
            "riskyWindow": {
              "from": "2019-07-04T13:33:03Z",
              "until": "2019-07-04T23:59:03Z"
            },
            "messageType": "M2"
        }
    ]
}
```

### Response Example: Message Type "M3" (with optional parameter):
```json
{
    "venues" : [
        {
            "id": "4WT59M5Y",
            "riskyWindow": {
              "from": "2019-07-04T13:33:03Z",
              "until": "2019-07-04T23:59:03Z"
            },
            "messageType": "M3",
            "optionalParameter": "07911 123456"
        }
    ]
}
```

#### Notes
- The server sends the date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
- Example: if the downstream data source has a granularity of a day, the time will be default to 00:00:00

## Message types and actions triggered in mobile apps (work in progress - placeholders for now)
- see also: "Risky Venue Messages Configuration-Download API"
- `M1`: type: to be defined, message: to be defined, actions (mobile app): to be defined
- `M2`: type: to be defined, message: to be defined, actions (mobile app): to be defined
- `M3`: type: to be defined, message: to be defined (click on phone number opens system dialog), actions (mobile app): to be defined
