# Epidemiological data exports to AAE (events)

Last updated on 05 Feb 2021. 

API group: Export (interface exposed by AAE and consumed by the England/Wales National Backend)

- Endpoint: HTTPS PUT ```https://<FQDN>/c19appdata/<timestamp epoc millis>_<random id>.json?feedName=Epidemiological```
- Authorization: 
  - Mutual TLS authentication (TLS client cert & keystore credentials in SecretsManager)
  - Subscription key (subscription key in SecretsManger)
- Request Headers:
  - Content-Type: application/json
  - Ocp-Apim-Trace: true
  - Ocp-Apim-Subscription-Key: ```SUBSCRIPTION_KEY```

## Scenario

Mobile clients send anonymous epidemiological data to the backend (without ```uuid``` field), which sends the data to AAE without significant delays or buffering.

The backend generates a ```uuid``` field (random value) to facilitate further upsteam processing.
 
## Mobile Payload Example (for event type ```exposureWindow```)

- Exposure windows sent to mobile immediately after encounter detection.
- Important (privacy): only one ```exposureWindowPositiveTest``` or ```exposureWindow``` event per export.

### Version 1

### Payload Example

```json
{
  "uuid": "0A0F811A-DA1E-4BAE-AB56-611A5DE4BBA3",
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

### Version 2

Includes additional isConsideredRisky field

### Payload Example

```json
{
  "uuid": "0A0F811A-DA1E-4BAE-AB56-611A5DE4BBA3",
  "metadata" : {
    "operatingSystemVersion" : "iPhone OS 13.5.1 (17F80)",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "iPhone11,2",
    "postalDistrict" : "A1",
    "localAuthority" : "E09000012"
  },
  "events": [{
    "type": "exposureWindow",
    "version": 2,
    "payload": {
      "isConsideredRisky": true,
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

## Mobile Payload Example (for event type ```exposureWindowPositiveTest```)

- Stored exposure windows sent to mobile after receiving a positive test.
- Important (privacy): only one ```exposureWindowPositiveTest``` or ```exposureWindow``` event per submission.

### Version 1

#### Payload Example 

```json
{
  "uuid": "0A0F811A-DA1E-4BAE-AB56-611A5DE4BBA3",
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
### Version 2

Includes additional requiresConfirmatoryTest field

#### Payload Example 

```json
{
  "uuid": "0A0F811A-DA1E-4BAE-AB56-611A5DE4BBA3",
  "metadata" : {
    "operatingSystemVersion" : "iPhone OS 13.5.1 (17F80)",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "iPhone11,2",
    "postalDistrict" : "A1",
    "localAuthority" : "E09000012"
  },
  "events": [{
    "type": "exposureWindowPositiveTest",
    "version": 2,
    "payload": {
      "testType": "unknown", 
      "requiresConfirmatoryTest": false,
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

### Version 3

Includes additional isConsideredRisky field

#### Payload Example 

```json
{
  "uuid": "0A0F811A-DA1E-4BAE-AB56-611A5DE4BBA3",
  "metadata" : {
    "operatingSystemVersion" : "iPhone OS 13.5.1 (17F80)",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "iPhone11,2",
    "postalDistrict" : "A1",
    "localAuthority" : "E09000012"
  },
  "events": [{
    "type": "exposureWindowPositiveTest",
    "version": 3,
    "payload": {
      "testType": "unknown", 
      "isConsideredRisky": true,
      "requiresConfirmatoryTest": false,
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

#### Validation (the backend guarantees the following properties of the JSON message sent to AAE)

* JSON is parseable
* Mandatory values (i.e. check existence of these properties):
  * `uuid`  
  * `metadata.operatingSystemVersion`
  * `metadata.latestApplicationVersion`
  * `metadata.deviceModel`
  * `metadata.postalDistrict`
  * `events`    
  * `events[*].type`
  * `events[*].version`
  * `events[*].payload`  

#### Rules (the mobile apps guarantee the following properties of the JSON message sent to AAE via the backend)
  
* Dates:
  * Date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
* Supported event types and versions:
  * Type: exposureWindow, version: 1
    * One submission per exposure window (i.e. ```events.length == 1```)
  * Type: exposureWindowPositiveTest, version: 1
    * One submission per exposure window (i.e. ```events.length == 1```)
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

* 2xx: Event successfully ingested
* 3xx, 4xx, 5xx: Event not ingested - retry (up to 2 times)
