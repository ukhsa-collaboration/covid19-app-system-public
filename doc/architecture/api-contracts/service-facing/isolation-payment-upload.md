# Isolation Payment API (Wales SIP Gateway-facing)

API group: [Upload](../../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Verify Isolation Payment Token: ```POST https://<FQDN>/isolation-payment/ipc-token/verify-token```
- Consume Isolation Payment Token: ```POST https://<FQDN>/isolation-payment/ipc-token/consume-token```

### Parameters

- FQDN: Target-environment specific CNAME of the Upload CloudFront distribution
- Authorization required and signatures NOT provided - see [API security](../security.md)
- Payload content-type: ```text/json```

## Example: Verify Isolation Payment Token

Isolation Payment Gateway verifies ipcToken passed in via URL query string parameter.

Endpoint: ```POST https://<FQDN>/isolation-payment/ipc-token/verify-token```

Request body:
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```
Request:
- ```ipcToken```: ```ipcToken``` returned by a previous "Create Isolation Payment Token" call
- ```contractVersion```: ```contractVersion``` version of API, not in use

Response body (```200 Success```):
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "valid",
  "riskyEncounterDate": "2020-08-14T21:57:00Z",
  "isolationPeriodEndDate": "2020-08-24T21:59:00Z",
  "createdTimestamp": "2020-08-14T21:59:00Z",
  "updatedTimestamp": "2020-08-24T21:59:30Z",
}
```
Response body (```422 Unprocessable Entity```) - when providing an invalid token:
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "invalid"
}
```

Response:
- ```contractVersion```: Version of the API. It is for future use (version in response hard-coded, version in request not validated)
- ```ipcToken```: Unique ID (generated from secure random source, 32 bytes, hex representation).
- ```state```: State of an ipcToken, can either be `valid` or `invalid`


## Example: Consume Isolation Payment Token (contractVersion 1)

Isolation Payment Gateway consumes ipcToken after successfully delivering the isolation payment claim to CTAS.

Endpoint: ```POST https://<FQDN>/isolation-payment/ipc-token/consume-token```

Request body:
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```

Request:
- ```ipcToken```: ```ipcToken``` returned by a previous "Create Isolation Payment Token" call
- ```contractVersion```: ```contractVersion``` version of API, not in use

Response body (```200 OK```):
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "invalid"
}
```
Response body (```422 Unprocessable Entity```) - when providing an invalid token:
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "invalid"
}
```
Response:
- ```contractVersion```: Version of the API. It is for future use (version in response hard-coded, version in request not validated)
- ```ipcToken```: Unique ID (generated from secure random source, 32 bytes, hex representation).
- ```state```: State of an ipcToken, can either be `valid` or `invalid`
