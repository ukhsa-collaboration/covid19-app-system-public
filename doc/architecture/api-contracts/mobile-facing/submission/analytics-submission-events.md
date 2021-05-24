# Epidemiological data submissions (events)

API group: [Submission](../../../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Submit Mobile Analytics Event: ```POST https://<FQDN>/submission/mobile-analytics-events```

### Parameters

- FQDN: Hostname can be different per API
- Authorization required and signatures provided - see [API security](../../security.md)
- Request Headers:
  - Content-Type: application/json

## Scenario

Mobile clients send anonymous epidemiological data to the backend (which sends the data to AAE).

## Mobile Payload (for event type ```exposureWindow``` version 1)

- Exposure windows sent to mobile immediately after encounter detection.
- Important (privacy): only one ```exposureWindowPositiveTest``` or ```exposureWindow``` event per submission.


## Mobile Payload (for event type ```exposureWindowPositiveTest```)

- Stored exposure windows sent to mobile after receiving a positive test.
- Important (privacy): only one ```exposureWindowPositiveTest``` or ```exposureWindow``` event per submission.

### Version 2

Includes additional requiresConfirmatoryTest field


#### Validation (backend)

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
  
#### Rules (mobile)  
  
* Dates:
  * Date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
* Supported event types and versions:
  * Type: exposureWindow, version: 1
    * Privacy rule: One submission per exposure window (i.e. ```events.length == 1```)
  * Type: exposureWindowPositiveTest, version: 1
    * Privacy rule: One submission per exposure window (i.e. ```events.length == 1```)
  * Type: exposureWindowPositiveTest, version: 2
    * One submission per exposure window (i.e. ```events.length == 1```)

##### Risk calculation version
The calculated risk score can vary between different OS/App versions.
Currently there are two risk calculation versions with the following mapping:

*Version 1*
- Apps or devices using EN API v1.5/1 (App version < 3.9, iOS devices < iOS 13.7)

*Version 2*
- Apps or devices using EN API v1.6/2 or higher (App version >= 3.9, iOS devices >= iOS 13.7)

**Verson 1 should never be sent by an app, as exposure windows are only available since version 2** 

### Test Type

```exposureWindowPositiveTest``` events contain a ```testType``` which can have a value of ```"LAB_RESULT"``` for a PCR test, ```"RAPID_RESULT"``` for LFD test or ```"unknown"```.

### HTTP response codes

| Status Code | Description |
| --- | --- |
| 2xx | Submission processed |
| 3xx | Submission not processed |
| 4xx | Submission not processed |
| 5xx | Submission not processed - retry (up to 2 times) |
