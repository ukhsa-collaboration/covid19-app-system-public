# Token API: Upload single test result (once)

API group: [Upload](../guidebook.md#system-apis-and-interfaces)

This API provides upload endpoints for UK wide integration of virology test result delivery to the NHS CV19 App System. Uploaded tests are distributed to mobile clients with a latency of min 2h, best we expect 4h, worst case 6 to 24 hours.

The endpoint URL has path elements specific to the external system using it, for instance `eng` for test results sent from the english system or `wls` for results sent by the welsh system.

Note: The Token API and the Test Lab API are conceptually the same endpoint but should not be called as part of the same flow. 
The key difference is that the Test Lab API expects a ctaToken as input (generated previously by our system) and the Token API creates and returns a ctaToken. 

## Endpoints

- System (England) posts a json test result: ```POST https://<FQDN>/upload/virology-test/eng-result-tokengen```
- System (Wales) posts a json test result: ```POST https://<FQDN>/upload/virology-test/wls-result-tokengen```
- System (Self Administered Lateral Flow Device) posts a json test result: ```POST https://<FQDN>/upload/virology-test/lfd-result-tokengen```

### Response Codes
  - `HTTP 200` ok
  - `HTTP 422` invalid json request body
  - `HTTP 500` internal server error
  
## Payloads

See also Test Lab API for valid testResult codes.

### System (England): test result upload AND ctaToken generation

```POST https://<FQDN>/upload/virology-test/eng-result-tokengen```
```json
{
    "testEndDate": "2020-04-23T00:00:00Z",
    "testResult": "NEGATIVE"
}
```

Response body
``` json
{
  "ctaToken": "1234abcd"
}
```

### System (Wales): test result upload AND ctaToken generation

```POST https://<FQDN>/upload/virology-test/wls-result-tokengen```
```json
{
    "testEndDate": "2020-05-23T00:00:00Z",
    "testResult": "POSITIVE"
}
```

Response body
``` json
{
  "ctaToken": "1234abcd"
}
```

### System (Self Administered Lateral Flow Device): test result upload AND ctaToken generation

```POST https://<FQDN>/upload/virology-test/lfd-result-tokengen```
```json
{
    "testEndDate": "2020-05-23T00:00:00Z",
    "testResult": "POSITIVE"
}
```

Response body
``` json
{
  "ctaToken": "1234abcd"
}
```

## Validation

- `ctaToken` Token must be valid according to the [Crockford and Damm](../../design/details/crockford-damm.md) algorithm.
- `testEndDate` ISO8601 format in UTC. Example: `2020-04-23T00:00:00Z`. Time is set to `0` to obfuscate test result relation to personal data
- `testResult` one of the following
  - eng `POSITIVE | NEGATIVE | VOID`
  - wls `POSITIVE | NEGATIVE | INDETERMINATE`
- Please note: INDETERMINATE and VOID are both treated as VOID by the mobile application - the behaviour could change in the future