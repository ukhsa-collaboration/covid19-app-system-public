# Isolation Payment API (Isolation Payment Gateway-facing)
 
Overview: 
- Direct synchronous Lambda invocation
- Verify Isolation Payment Token (Lambda Function name): ```<target-environment>-ipc-token-verify```
- Consume Isolation Payment Token (Lambda Function name): ```<target-environment>-ipc-token-consume```
- Request & response: JSON
- Authentication/authorization: IAM (Cross account IAM trust policy / lambda invoke policy)
- Error handling: Lambda must throw an Exception in case of JSON parsing errors, validation errors, DynamoDB access errors, etc.
- Relaxed JSON parsing (backwards- and forwards compatibility): Additional unexpected JSON properties are ignored

## Example: Verify Isolation Payment Token (contractVersion 1)

Isolation Payment Gateway verifies ipcToken passed in via URL query string parameter.

Request:
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```

Response (success):
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
All fields: always present (not null, not empty)

Response (error - token not found or token in wrong state, e.g. token already consumed):
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "invalid"
}
```

All fields: always present (not null, not empty)

Notes:
- ```contractVersion``` is for future use (version in response hard-coded, version in request not validated)

## Example: Consume Isolation Payment Token (contractVersion 1)

Isolation Payment Gateway consumes ipcToken after successfully delivering the isolation payment claim to CTAS.

Request:
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac"
}
```

Response (success):
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "consumed"
}
```

All fields: always present (not null, not empty)

Response (error - token not found or token in wrong state):
```json
{
  "contractVersion": 1,
  "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
  "state": "invalid"
}
```

All fields: always present (not null, not empty)

Notes:
- ```contractVersion``` is for future use (version in response hard-coded, version in request not validated)
