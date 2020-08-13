# Virology Testing API

Version V3.0, 2020-08-08

API group: [Submission](../api-patterns.md#Submission)

The Virology Testing API is a mobile facing API and hence, to reduce roundtrip times, does not follow a RESTful approach to API design. Its basic principle is to provide HTTP endpoints to retrieve URLs and tokens for the interaction with the website and to enable retrieving test results later.

The token in V2 is a base32 encoded 40byte random number, using the Crockford encoding. Javascript for validation is attached below.

## Endpoints for mobile app

### URLs and HTTP verbs

- Order a home kit test: ```[POST] https://<FQDN>/virology-test/home-kit/order```
- Register home kit test: ```[POST] https://<FQDN>/virology-test/home-kit/register```
- Poll for test results: ```[POST] https://<FQDN>/virology-test/results```
- Payload content-type: ```application/json```
- All endpoints of this API
  - FQDN: Hostname can be different per API
  - Authorization: ```Authorization: Bearer <API KEY>```
    - One API KEY for all mobile phone-facing APIs

### Response Codes

- Order a home kit test
  - `HTTP 200` ok
  - `HTTP 500` internal server error
- Register home kit test
  - `HTTP 200` ok
  - `HTTP 500` internal server error
- Poll for test results
  - `HTTP 200` ok
  - `HTTP 204` no result yet
  - `HTTP 404` polling token not found
  - `HTTP 422` invalid json request body
  - `HTTP 500` internal server error
  
### Response Headers
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Payload Examples

### Response for ordering a home kit test / registering a home kit test

POST https://<FQDN>/virology-test/home-kit/order

POST https://<FQDN>/virology-test/home-kit/register

Request body: empty

Response body:
```json
{
    "websiteUrlWithQuery": "https://self-referral.test-for-coronavirus.service.gov.uk/cta-start?ctaToken=tbdfjaj0",
    "tokenParameterValue": "tbdfjaj0", /* only to be displayed in app */
    "testResultPollingToken" : "61EEFD4B-E903-4294-B595-B1D491134E3D", /* see result polling below */
    "diagnosisKeySubmissionToken": "6B162698-ADC5-47AF-8790-71ACF770FFAF" /* see diagnosis-key-submission.md */
}
```

### Poll for test result using the token from the ordering requests

The response payload returns the values from the TestLab csv

POST https://<FQDN>/virology-test/results

Request body:
```json
{
    "testResultPollingToken": "61EEFD4B-E903-4294-B595-B1D491134E3D"
}
```

Response body:
```json
{
    "testEndDate": "2020-04-23T00:00:00.0000000Z",
    "testResult": "POSITIVE"
}
```
or
```json
{
    "testEndDate": "2020-04-23T00:00:00.0000000Z",
    "testResult": "NEGATIVE"
}
```
or
```json
{
    "testEndDate": "2020-04-23T00:00:00.0000000Z",
    "testResult": "VOID"
}
```
## Notes and Links

- Confluence: Search Virology
- [Conceptual system flow](../../architecture/ag-architecture-guidebook.md#system-flow-request-virology-testing-and-get-result-using-a-temporary-token)


## Javascript to validate Token

Taken fom the V2 documentation

```javascript

function validateAppRefCode(code) {
  var CROCKFORD_BASE32 = "0123456789abcdefghjkmnpqrstvwxyz";
  var cleaned = code.toLowerCase().replace(/il/g, "1").replace(/o/g, "0").replace(/u/g, "v").replace(/[- ]/g, "");
  var i;
  var checksum = 0;
  var digit;

  for (i = 0; i < cleaned.length; i++) {
    digit = CROCKFORD_BASE32.indexOf(cleaned.charAt(i));
    checksum = damm32(checksum, digit);
  }

  return checksum == 0;
}

function damm32(checksum, digit) {
  var DAMM_MODULUS = 32;
  var DAMM_MASK = 5;

  checksum ^= digit;
  checksum *= 2;
  if (checksum >= DAMM_MODULUS) {
    checksum = (checksum ^ DAMM_MASK) % DAMM_MODULUS;
  }
  return checksum;
}
```
