# Test Lab API: Upload single test result (once)

API group: [Upload](../guidebook.md#system-apis-and-interfaces)

This API provides upload endpoints for UK wide integration of virology test result delivery to the NHS CV19 App System. Uploaded tests are distributed to mobile clients with a latency of min 2h, best we expect 4h, worst case 6 to 24 hours.

The endpoint URL has path elements specific to the external system using it, for instance `npex` for test results sent from the english test results database or `fiorano` for results sent by the welsh integration component, operated and maintained by the NWIS Integration service.

Note: The Token API and the Test Lab API are conceptually the same endpoint but should not be called as part of the same flow. 
The key difference is that the Test Lab API expects a ctaToken as input (generated previously by our system) and the Token API creates and returns a ctaToken.

## Endpoints

- NPEx posts a json test result: ```POST https://<FQDN>/upload/virology-test/npex-result```
- Fiorano posts a json test result: ```POST https://<FQDN>/upload/virology-test/fiorano-result```

### Response Codes
  - `HTTP 202` accepted test result
  - `HTTP 400` given cta token is not registered in the system
  - `HTTP 409` test result already exists for the given ctaToken
  - `HTTP 422` invalid json request body
  - `HTTP 500` internal server error
  
## Payloads

### NPEx test result upload

```POST https://<FQDN>/upload/virology-test/npex-result```
```json
{
    "ctaToken": "t1qjee7p",
    "testEndDate": "2020-04-23T00:00:00Z",
    "testResult": "NEGATIVE"
}
```

```POST https://<FQDN>/upload/virology-test/npex-result```
```json
{
    "ctaToken": "64hr743d",
    "testEndDate": "2020-07-23T00:00:00Z",
    "testResult": "VOID"
}
```

### Fiorano test result upload

```POST https://<FQDN>/upload/virology-test/fiorano-result```
```json
{
    "ctaToken": "9bknrr20",
    "testEndDate": "2020-05-23T00:00:00Z",
    "testResult": "POSITIVE"
}
```

```POST https://<FQDN>/upload/virology-test/fiorano-result```
```json
{
    "ctaToken": "hj3dz378",
    "testEndDate": "2020-05-23T00:00:00Z",
    "testResult": "INDETERMINATE"
}
```

### Validation

- `ctaToken` Token must be valid according to the [Crockford and Damm](../../design/details/crockford-damm.md) algorithm.
- `testEndDate` ISO8601 format in UTC. Example: `2020-04-23T00:00:00Z`. Time is set to `0` to obfuscate test result relation to personal data
- `testResult` one of the following
  - NPEx `POSITIVE | NEGATIVE | VOID`
  - Fiorano `POSITIVE | NEGATIVE | INDETERMINATE`
- **One-time upload only**: we don't accept multiple uploads with the same ctaToken.
- Please note: INDETERMINATE and VOID are both treated as VOID by the mobile application - the behaviour could change in the future

## Notes

- Note that **distribution to mobile clients** depends on system latency *and* mobile systems background polling scheduler. So it might take between 6h to 24h until uploaded test results are finally delivered to the mobile app.
  
- **Expected Fiorano load**
  - Subset of all test results (those wit a cta token)
  - numbers TBD

- **Expected NPEx load**, Mail (2020-07-21)
  - Deloitte testing process, all results: Daily - Up to 100,000 results, Hourly - Up to 20,000. Test Lab API will receive only a subset
  - results aren't evenly distributed throughout the day
  - tend to receive large batches of results in close succession
  - only be sending a subset of these results to the Test Lab API
  (the ones where the request contained a 'ctaToken')

- **Requirements vs proposed rate limits**
  - NPEx can configure rate limits, so don't exceed a predefined message rate
  - NPEx still retries with exponential backoff on 429 response

| | daily | daily/average RPS	| hourly	| hourly/average RPS |
|-| ------ | -----------------| -------- | ----------------- |  
|NPEx all results | 100000|	1,16 |	20000	| 5,56 |
|Fiorano all results | TBD|	TBD |	TBD	| TBD |
|Test Lab API proposed | 	8640000	| 100 | 	360000	|100 |
