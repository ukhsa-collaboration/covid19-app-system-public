# Isolation Payment Claim Token Consumption

> API Pattern: [Upload](../../../api-patterns.md#upload)

## HTTP Request and Response
> SIP Gateway Wales only
- Verify Isolation Payment Token: ```POST https://<FQDN>/isolation-payment/ipc-token/verify-token```
- Consume Isolation Payment Token: ```POST https://<FQDN>/isolation-payment/ipc-token/consume-token```

### Parameters

- FQDN: Target-environment specific CNAME of the Upload CloudFront distribution
- Authorization required and signatures NOT provided - see [API security](../../../api-security.md)
- Payload content-type: ```text/json```

## Direct Lambda Invocation
> SIP Gateway England only
- Verify Isolation Payment Token (Lambda Function name): ```<target-environment>-ipc-token-verify```
- Consume Isolation Payment Token (Lambda Function name): ```<target-environment>-ipc-token-consume```

### Parameters
- Request & response: JSON
- Authentication/authorization: IAM (Cross account IAM trust policy / lambda invoke policy)
- Error handling: Lambda must throw an Exception in case of JSON parsing errors, validation errors, DynamoDB access errors, etc.
- Relaxed JSON parsing (backwards- and forwards compatibility): Additional unexpected JSON properties are ignored

## Scenario
### API V1: Verify Isolation Payment Token

Isolation Payment Gateway verifies ipcToken.

#### Request Payload Example
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```

- ```contractVersion```: version of API
- ```ipcToken```: IPC token, previously created

#### Response Payload Example (success)
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "valid",
  "riskyEncounterDate": "2020-08-14T21:57:00Z",
  "isolationPeriodEndDate": "2020-08-24T21:59:00Z",
  "createdTimestamp": "2020-08-14T21:59:00Z",
  "updatedTimestamp": "2020-08-24T21:59:30Z"
}
```

- ```contractVersion```: version of the API
- ```ipcToken```: IPC token
- ```state```: state of the IPC Token, can either be `valid` or `invalid`

All fields are always present (not null, not empty)

#### Response Payload Example (error - token not found or token in wrong state, e.g. token already consumed)
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "invalid"
}
```

All fields are always present (not null, not empty)

### API V1: Consume Isolation Payment Token

Isolation Payment Gateway consumes ipcToken after successfully delivering the isolation payment claim to CTAS.

#### Request Payload Example
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```
- ```contractVersion```: ```contractVersion``` version of API
- ```ipcToken```: ```ipcToken```  IPC token, previously created

#### Response Payload Example (success)
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "consumed"
}
```
- ```contractVersion```: version of the API
- ```ipcToken```: IPC token, previously created
- ```state```: state of the IPC Token, can either be `consumed` or `invalid`

All fields are always present (not null, not empty)

#### Response Payload Example (error - token not found or token in wrong state)
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "invalid"
}
```

All fields are always present (not null, not empty)

### HTTP Response Codes

- `200 OK` ok
- `422 Unprocessable Entity` invalid json request body
- `500 Internal Server Error` internal server error
- `503 Service Unavailable` maintenance mode

#### Maintenance Mode

When the service is in [maintenance mode](../../../../design/details/api-maintenance-mode.md), all services will return `503 Service Unavailable`. Maintenance mode is used when performing operations like backup and restore of the service data.

### Notes
- ```contractVersion``` is for future use only (the version in the request is not validated, the version in the response is hard-coded)

- For IPC Token creation see [Isolation Payment Claim Submission](../../mobile-facing/submission/isolation-payment-claim-token-submission.md)
