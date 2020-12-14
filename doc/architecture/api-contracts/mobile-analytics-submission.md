# Epidemiological data submissions (events)

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Submit Mobile Analytics Event: ```POST https://<FQDN>/submission/mobile-analytics-events```

### Parameters

- FQDN: Hostname can be different per API
- Authorization required and signatures provided - see [API security](./security.md)
- Request Headers:
  - Content-Type: application/json

## Scenario

Mobile clients send anonymous epidemiological data to the backend (which sends the data to AAE).

## Mobile Payload Example (for event type ```exposureWindow``` version 1)

- Exposure windows sent to mobile immediately after encounter detection.
- Important (privacy): only one ```exposureWindowPositiveTest``` or ```exposureWindow``` event per submission.

### Request Payload Example

```json
{
  "metadata" : {
    "operatingSystemVersion" : "iPhone OS 13.5.1 (17F80)",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "iPhone11,2",
    "postalDistrict" : "A1",
    "localAuthority" : "E09000012"
  },
  "events": [{
    "type": "exposureWindow",
    "version": 1,
    "payload": {
      "date": "2020-08-24T21:59:00Z",
      "infectiousness": "high|none|standard",
      "scanInstances": [
        {
          "minimumAttenuation": 1,
          "secondsSinceLastScan": 5,
          "typicalAttenuation": 2
        }
      ],
      "riskScore": 150,
      "riskCalculationVersion": 2
    }
  }]
}
```

## Mobile Payload Example (for event type ```exposureWindowPositiveTest``` version 1)

- Stored exposure windows sent to mobile after receiving a positive test.
- Important (privacy): only one ```exposureWindowPositiveTest``` or ```exposureWindow``` event per submission.
- FYI: ```testType``` is for future use.

### Request Payload Example

```json
{
  "metadata" : {
    "operatingSystemVersion" : "iPhone OS 13.5.1 (17F80)",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "iPhone11,2",
    "postalDistrict" : "A1",
    "localAuthority" : "E09000012"
  },
  "events": [{
    "type": "exposureWindowPositiveTest",
    "version": 1,
    "payload": {
      "testType": "unknown",
      "date": "2020-08-24T21:59:00Z",
      "infectiousness": "high|none|standard",
      "scanInstances": [
        {
          "minimumAttenuation": 1,
          "secondsSinceLastScan": 5,
          "typicalAttenuation": 2
        }
      ],
      "riskScore": 150,
      "riskCalculationVersion": 2
    }
  }]
}
```

#### Validation (backend)

* JSON is parseable
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

##### Risk calculation version
The calculated risk score can vary between different OS/App versions.
Currently there are two risk calculation versions with the following mapping:

*Version 1*
- Apps or devices using EN API v1.5/1 (App version < 3.9, iOS devices < iOS 13.7)

*Version 2*
- Apps or devices using EN API v1.6/2 or higher (App version >= 3.9, iOS devices >= iOS 13.7)

**Verson 1 should never be sent by an app, as exposure windows are only available since version 2** 

### HTTP response codes

| Status Code | Description |
| --- | --- |
| 2xx | Submission processed |
| 3xx | Submission not processed |
| 4xx | Submission not processed |
| 5xx | Submission not processed - retry (up to 2 times) |
