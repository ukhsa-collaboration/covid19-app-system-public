# Isolation Payment API (Mobile-facing)

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Create Isolation Payment Token: ```POST https://<FQDN>/isolation-payment/ipc-token/create```
- Update Isolation Payment Token: ```POST https://<FQDN>/isolation-payment/ipc-token/update```

### Parameters

- FQDN: Target-environment specific CNAME of the Mobile Submission CloudFront distribution
- Authorization required and signatures provided - see [API security](./security.md)
- Request and response Content-Type: ```application/json```
- See also: standard attributes of mobile submission APIs (API key, response body signing, throttling error code, etc.)
- Relaxed JSON parsing (backwards and forwards compatibility): Additional unexpected JSON properties are ignored

## Example: Create Isolation Payment Token

Mobile app creates ```ipcToken``` after start of self-isolation period (the generated token cannot be consumed yet by the Isolation Payment Gateway).

Endpoint: ```POST https://<FQDN>/isolation-payment/ipc-token/create```

Request body:
```json
{
  "country": "England|Wales"
}
```
Request:
- ```country```: Origin country of request

Response body (```201 Created```):
```json
{
  "isEnabled": true,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```

Response:
- ```isEnabled```: If country is in the whitelisted countries list
- ```ipcToken```: Unique ID (generated from secure random source, 32 bytes, hex representation).

## Example: Update Isolation Payment Token

Mobile app updates ```ipcToken``` when the user requests an isolation payment (the token can be consumed by the Isolation Payment Claim website after a succeed call).

Endpoint: ```POST https://<FQDN>/isolation-payment/ipc-token/update```

Request body:
```json
{
    "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
    "riskyEncounterDate": "2020-08-24T21:59:00Z",
    "isolationPeriodEndDate": "2020-08-24T21:59:00Z"
}
```

Request:
- ```ipcToken```: ```ipcToken``` returned by a previous "Create Isolation Payment Token" call
- ```riskyEncounterDate``` and ```isolationPeriodEndDate```: ISO-8601 timestamp in ```YYYY-MM-DD'T'hh:mm:ssZ``` format

Response body (```200 OK```):
```json
{
  "websiteUrlWithQuery": "https://<FQDN to be defined>/<path to be defined>?ipcToken=7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```

Response:
- ```websiteUrlWithQuery```: To be opened in the mobile web browser (to pass the ```ipcToken``` to the Isolation Payment Gateway)
- The request must succeed (and will return the website URL with the *provided* token id) even if the provided token id is invalid
- Response ```400 Bad Request``` if the request text cannot be parsed or if JSON properties are missing or of the wrong type
