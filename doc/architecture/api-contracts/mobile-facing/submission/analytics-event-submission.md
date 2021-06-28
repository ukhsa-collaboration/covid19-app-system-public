# Epidemiological Event Submission

> API Pattern: [Submission](../../../api-patterns.md#submission)

## HTTP Request and Response

- Submit Mobile Analytics Event: ```POST https://<FQDN>/submission/mobile-analytics-events```

### Parameters

- FQDN: Hostname can be different per API
- Authorization required and signatures provided - see [API security](../../../api-security.md)
- Request Headers:
  - Content-Type: application/json

## Scenario

Mobile clients send anonymous epidemiological data to the backend (which sends the data to AAE).

### Mobile Payload (for event type ```exposureWindow```)

- Exposure windows sent to mobile immediately after encounter detection.
- Important (privacy): only one ```exposureWindow``` event per submission.


### Mobile Payload (for event type ```exposureWindowPositiveTest```)

- Stored exposure windows sent to mobile after receiving a positive test.
- Important (privacy): only one ```exposureWindowPositiveTest```  event per submission.
- Version 2 of`exposureWindowPositiveTest`
  - includes additional `requiresConfirmatoryTest` field

### Validation (backend)

* JSON is parsable
* Mandatory values (i.e. check existence of these properties):
  * `metadata.operatingSystemVersion`
  * `metadata.latestApplicationVersion`
  * `metadata.deviceModel`
  * `metadata.postalDistrict`
  * `events`      
  * `events[*].type`
  * `events[*].version`
  * `events[*].payload`  
  
### Rules (mobile)  
  
* Dates:
  * Date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
* Supported event types and versions:
  * Type: exposureWindow, version: 1
    * Privacy rule: One submission per exposure window (i.e. ```events.length == 1```)
  * Type: exposureWindowPositiveTest, version: 1
    * Privacy rule: One submission per exposure window (i.e. ```events.length == 1```)
  * Type: exposureWindowPositiveTest, version: 2
    * Privacy rule: One submission per exposure window (i.e. ```events.length == 1```)

### Risk calculation version
The calculated risk score can vary between different OS/App versions.
Currently there are two risk calculation versions with the following mapping:

Risk Calculation *V1*
- Apps or devices using EN API v1.5/1 (App version < 3.9, iOS devices < iOS 13.7)

Risk Calculation *V2*
- Apps or devices using EN API v1.6/2 or higher (App version >= 3.9, iOS devices >= iOS 13.7)

Risk Calculation V1 **should never be sent by an app**, as exposure windows are only available since Risk Calculation V2

### Test Types

The ```exposureWindowPositiveTest``` events contain a ```testType``` which can be one of the following:

| Test Type | Description |
| --- | --- |
| ```LAB_RESULT``` | PCR test|
|```RAPID_RESULT```| LFD test|
|```unknown```     | unknown test|


### HTTP Response Codes

| Status Code | Description |
| --- | --- |
| `2xx` | Submission processed |
| `3xx` | Submission not processed |
| `4xx` | Submission not processed |
| `5xx` | Submission not processed - retry (up to 2 times) |
