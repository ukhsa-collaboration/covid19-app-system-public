# Token API V1 (deprecated) and V2: Upload single PCR- or LFD test result (once)

API group: [Upload](../../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Test result upload (V1 - **deprecated - will be only supported during a transition period**)
  - ```POST https://<FQDN>/upload/virology-test/<SOURCE>-result-tokengen```
  - Can only be used to upload PCR test results (or LFD-like test results to be treated as PCR test results - depending on the current policy)
- Test result upload (V2)
  - ```POST https://<FQDN>/upload/virology-test/v2/<SOURCE>-result-tokengen```
  - Can be used to upload LFD-like and PCR-like test results
- Test token status (V2)
  - ```POST https://<FQDN>/upload/virology-test/v2/<SOURCE>-result-tokenstatus```
- Valid SOURCE values: 
  - `eng`: BSA
  - `wls`: PHW

### Parameters

- Authorization required and signatures NOT provided - see [API security](../security.md)

## Scenario

This API provides upload endpoints for UK wide integration of virology test result delivery to the NHS CV19 App System. 

The endpoint URL has path elements specific to the external system using it (SOURCE). All endpoints behave identical, the source path element is required to setup IP whitelisting rules.

Note: The Token API and the Test Lab API are conceptually the same endpoint but should not be called as part of the same flow (mutually exclusive for the same test result).  The key difference is that the Test Lab API expects a ctaToken as input (generated previously by our system) and the Token API creates and returns a ctaToken. 
  
## Payloads

See also Test Lab API for valid testResult codes.

### Example (V1 API): Negative PCR test result upload AND ctaToken generation

```POST https://<FQDN>/upload/virology-test/eng-result-tokengen```

Request body:
```json
{
    "testEndDate": "2020-04-23T00:00:00Z",
    "testResult": "NEGATIVE"
}
```

Response body:
``` json
{
  "ctaToken": "1234abcd"
}
```

### Example (V2 API): Positive LFD test result upload AND ctaToken generation

```POST https://<FQDN>/upload/virology-test/v2/wls-result-tokengen```

Request body:
```json
{
    "testEndDate": "2020-05-23T00:00:00Z",
    "testResult": "POSITIVE",
    "testKit": "RAPID_RESULT"
}
```

Response body:
``` json
{
  "ctaToken": "1234abcd"
}
```

### Example (V2 API): Checking the status of a token

```POST https://<FQDN>/upload/virology-test/v2/wls-result-tokenstatus```

Request body:
```json
{
    "ctaToken": "1234abcd"
}
```

Response body:
```json
{
    "tokenStatus": "consumable|other"
}
```
## Validation

V1 and V2 APIs


- `testEndDate` ISO8601 format in UTC. Example: `2020-04-23T00:00:00Z`. Time is set to `0` to obfuscate test result relation to personal data
- `testResult` one of the following
  - `POSITIVE`
  - `NEGATIVE`
  - `VOID`
  - `INDETERMINATE`
  - `PLOD` (**testKit must be `LAB_RESULT`**)
- Please note: `INDETERMINATE` and `VOID` are both treated as `VOID` by the mobile application - the behaviour could change in the future


V1 API

- `testKit` field **MUST NOT** be present
  - Test result will be treated as `LAB_RESULT`

V2 API

- `testKit`  one of the following
  - `LAB_RESULT`  
  - `RAPID_RESULT` (**only positive test results are supported**)
  - `RAPID_SELF_REPORTED` (**only positive test results are supported**)
    

### Response Codes

  - `HTTP 200` ok
  - `HTTP 422` invalid json request body
  - `HTTP 500` internal server error

### Maintenance Mode

When the service is in maintenance mode, all services will return 503. Maintenance mode is used when performing operations like backup and restore of the service data.

## Notes

- Note that **distribution to mobile clients** depends on system latency *and* mobile systems background polling scheduler. So it might take between 6h to 24h until uploaded test results are finally delivered to the mobile app.
