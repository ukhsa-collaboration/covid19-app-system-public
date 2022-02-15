# Virology Test Order Submission & Result Retrieval

> API Pattern: [Submission](../../../api-patterns.md#submission)

## HTTP Request and Response

### V1 API (deprecated)

Order test via the app:
- Order a home kit test: ```POST https://<FQDN>/virology-test/home-kit/order```
- Register home kit test (alias for order): ```POST https://<FQDN>/virology-test/home-kit/register``` 

Retrieve the result of a test ordered via the app:
- Poll for test results: ```POST https://<FQDN>/virology-test/results```
- Theoretically, LFD test results could also be returned to old app versions (old app versions can not distinguish between PCR and LFD results)

### V2 API

Order test via the app:
- Order a home kit test: ```POST https://<FQDN>/virology-test/v2/order```

Retrieve the result of a test ordered via the app:
- Poll for test results: ```POST https://<FQDN>/virology-test/v2/results```
- Theoretically, LFD test results could also be returned

### Parameters

- Authorization required and signatures provided - see [API security](../../../api-security.md)
- Payload content-type: ```application/json```

## Scenarios

The Virology Testing API provides HTTP endpoints to retrieve URLs and tokens for the interaction with the virology testing website and to enable retrieving test results later.

Virology tokens follow the [Crockford and Damm](../../../../design/details/crockford-damm.md) algorithm.

Please note: testResult INDETERMINATE reported by Fiorano will be delivered as VOID to the mobile application - the behaviour could change in the future.

### V1 and V2 API: Ordering a home kit test / registering a home kit test

```POST https://<FQDN>/virology-test/home-kit/order``` (deprecated)

```POST https://<FQDN>/virology-test/home-kit/register``` (deprecated)

```POST https://<FQDN>/virology-test/v2/order```

#### Request Payload
empty

#### Response Payload Example
```json
{
    "websiteUrlWithQuery": "https://self-referral.test-for-coronavirus.service.gov.uk/cta-start?ctaToken=tbdfjaj0",
    "tokenParameterValue": "tbdfjaj0",
    "testResultPollingToken" : "61EEFD4B-E903-4294-B595-B1D491134E3D",
    "diagnosisKeySubmissionToken": "6B162698-ADC5-47AF-8790-71ACF770FFAF",
}
```
* `tokenParameterValue` is only to be displayed in the app. See bellow for more info about the `testResultPollingToken` and `diagnosisKeySubmissionToken` fields. 


### V1 API: Poll for test result using the token from ordering or registering a test kit

```POST https://<FQDN>/virology-test/results``` (deprecated)

#### Request Payload Example
```json
{
    "testResultPollingToken": "61EEFD4B-E903-4294-B595-B1D491134E3D"
}
```

#### Response Payload Example
```json
{
    "testEndDate": "2020-04-23T00:00:00Z",
    "testResult": "POSITIVE"|"NEGATIVE"|"VOID"|"PLOD"
}
```

### V2 API: Poll for a test result using the token from ordering or registering a test kit

```POST https://<FQDN>/virology-test/v2/results```

#### Request Payload Example
```json
{
    "testResultPollingToken": "61EEFD4B-E903-4294-B595-B1D491134E3D",
    "country": "England"|"Wales"
}
```

#### Response Payload Example
```json
{
    "testEndDate": "2020-04-23T00:00:00Z",
    "testResult": "POSITIVE"|"NEGATIVE"|"VOID"|"PLOD",
    "testKit": "LAB_RESULT"|"RAPID_RESULT"|"RAPID_SELF_REPORTED",
    "diagnosisKeySubmissionSupported": true|false,
    "requiresConfirmatoryTest": true|false,
    "confirmatoryDayLimit": null|-1|0|>=1,
    "shouldOfferFollowUpTest": true|false
}
```
- `requiresConfirmatoryTest` indicates whether the user should be taken through the confirmatory test journey or not.
- `confirmatoryDayLimit` the value can be:
  - `null`: it does not apply to the current journey
  - `-1`: no window
  - `0`: window is 0 days
  - `>=1`: non-zero day window
  - **Note**: `confirmatoryDayLimit` value must be `null` if `requiresConfirmatoryTest` is equal to `false`
- `shouldOfferFollowUpTest` indicates whether the user should be offered a follow-up test
  - **Note:**: `requiresConfirmatoryTest` value can not be `false` when `shouldOfferFollowUpTest` is equal to `true`

### HTTP Response Codes

- Order a home kit test / Register home kit test
  - `200 OK` ok
  - `500 Internal Server Error` internal server error
- Poll for test results
  - `200 OK` ok
  - `204 No Content` no result yet
  - `404 Not Found` polling token not found (also for V1: polling token refers to a LFD test result - depending on the current policy)
  - `422 Unprocessable Entity` invalid json request body
  - `500 Internal Server Error` internal server error

#### Maintenance Mode

When the service is in [maintenance mode](../../../../design/details/api-maintenance-mode.md), all services will return `503 Service Unavailable`. Maintenance mode is used when performing operations like backup and restore of the service data.

### Notes

- See [Key User Journeys](../../../journeys.md) for conceptual system flows
