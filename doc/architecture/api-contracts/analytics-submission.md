# Daily mobile analytics submission

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

- Endpoint schema: ```https://<FQDN>/submission/mobile-analytics```
  - FQDN: Hostname can be different per API
- Authorization: ```Authorization: Bearer <API KEY>```
  - One API KEY for all mobile phone-facing APIs
- Extensibility: new Key-Value Pairs can be added to the request without breaking the API contract

## Scenario
Mobile clients collect and send app analytics periodically (roughly once per 24h) to the backend.
 
The request payload is received by the analytics submission aws lambda, which in turn is responsible for validating it 
and publishing it into an s3 bucket with a unique enough name (`<unixtimestamp>_<uuid>`). 

## Mobile Payload Example

### iOS Payload Example
```json
{
  "metadata" : {
    "operatingSystemVersion" : "iPhone OS 13.5.1 (17F80)",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "iPhone11,2",
    "postalDistrict" : "A1"
  },
  "analyticsWindow" : {
    "endDate" : "2020-07-28T22:59:00Z",
    "startDate" : "2020-07-27T23:00:00Z"
  },
  "metrics" : {
    "cumulativeDownloadBytes" : 140000000,
    "cumulativeUploadBytes" : 140000000,
    "cumulativeCellularDownloadBytes" : 80000000,
    "cumulativeCellularUploadBytes" : 70000000,
    "cumulativeWifiDownloadBytes" : 60000000,
    "cumulativeWifiUploadBytes" : 50000000,
    "checkedIn" : 1,
    "canceledCheckIn" : 1,
    "receivedVoidTestResult" : 1,
    "isIsolatingBackgroundTick" : 1,
    "hasHadRiskyContactBackgroundTick" : 1,
    "receivedPositiveTestResult" : 1,
    "receivedNegativeTestResult" : 1,
    "hasSelfDiagnosedPositiveBackgroundTick" : 1,
    "completedQuestionnaireAndStartedIsolation" : 1,
    "encounterDetectionPausedBackgroundTick" : 1,
    "completedQuestionnaireButDidNotStartIsolation" : 1,
    "totalBackgroundTasks" : 1,
    "runningNormallyBackgroundTick" : 1,
    "completedOnboarding" : 1
  },
  "includesMultipleApplicationVersions" : false
}
```

### Android Payload Example
```json
{
  "metadata" : {
    "operatingSystemVersion" : "29",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "HUAWEI LDN-L21",
    "postalDistrict" : "A1"
  },
  "analyticsWindow" : {
    "endDate" : "2020-07-28T22:59:00Z",
    "startDate" : "2020-07-27T23:00:00Z"
  },
  "metrics" : {
    "cumulativeDownloadBytes" : 140000000,
    "cumulativeUploadBytes" : 140000000,
    "checkedIn" : 1,
    "canceledCheckIn" : 1,
    "receivedVoidTestResult" : 1,
    "isIsolatingBackgroundTick" : 1,
    "hasHadRiskyContactBackgroundTick" : 1,
    "receivedPositiveTestResult" : 1,
    "receivedNegativeTestResult" : 1,
    "hasSelfDiagnosedPositiveBackgroundTick" : 1,
    "completedQuestionnaireAndStartedIsolation" : 1,
    "encounterDetectionPausedBackgroundTick" : 1,
    "completedQuestionnaireButDidNotStartIsolation" : 1,
    "totalBackgroundTasks" : 1,
    "runningNormallyBackgroundTick" : 1,
    "completedOnboarding" : 1
  },
  "includesMultipleApplicationVersions" : false
}
```
#### Validation
* date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
* Numeric values must not be null, except for the cumulativeDownloadBytes and cumulativeUploadBytes fields from android devices
* Boolean values must not be null
* Nullable values: `cumulativeDownloadBytes`, `cumulativeUploadBytes`

### Responses
| Status Code | Description |
| --- | --- |
| 200 | Submission processed |
| 400 | Bad request could not process payload |
