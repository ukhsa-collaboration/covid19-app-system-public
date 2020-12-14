# Daily mobile analytics submission

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Submit Mobile Analytics: ```POST https://<FQDN>/submission/mobile-analytics```

### Parameters

- FQDN: Target-environment specific CNAME of the Mobile Submission CloudFront distribution 
- Authorization required and signatures NOT provided - see [API security](./security.md)
- Extensibility: new Key-Value Pairs can be added to the request without breaking request processing

## Scenario

Mobile clients collect and send app analytics periodically (roughly once per 24h) to the backend.
 
## Example: Submit Mobile Analytics

### iOS Request Payload Example
```json
{
  "metadata" : {
    "operatingSystemVersion" : "iPhone OS 13.5.1 (17F80)",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "iPhone11,2",
    "postalDistrict" : "A1",
    "localAuthority" : "E09000012"
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
    "completedOnboarding" : 1,
    "receivedVoidTestResultEnteredManually" : 1,
    "receivedPositiveTestResultEnteredManually" : 1,
    "receivedNegativeTestResultEnteredManually" : 1,
    "receivedVoidTestResultViaPolling" : 1,
    "receivedPositiveTestResultViaPolling" : 1,
    "receivedNegativeTestResultViaPolling" : 1,
    "hasSelfDiagnosedBackgroundTick": 1,
    "hasTestedPositiveBackgroundTick": 1,
    "isIsolatingForSelfDiagnosedBackgroundTick": 1,
    "isIsolatingForTestedPositiveBackgroundTick": 1,
    "isIsolatingForHadRiskyContactBackgroundTick": 1
  },
  "includesMultipleApplicationVersions" : false
}
```

### Android Request Payload Example
```json
{
  "metadata" : {
    "operatingSystemVersion" : "29",
    "latestApplicationVersion" : "3.0",
    "deviceModel" : "HUAWEI LDN-L21",
    "postalDistrict" : "A1",
    "localAuthority" : "E09000012"
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
    "completedOnboarding" : 1,
    "receivedVoidTestResultEnteredManually" : 1,
    "receivedPositiveTestResultEnteredManually" : 1,
    "receivedNegativeTestResultEnteredManually" : 1,
    "receivedVoidTestResultViaPolling" : 1,
    "receivedPositiveTestResultViaPolling" : 1,
    "receivedNegativeTestResultViaPolling" : 1,
    "hasSelfDiagnosedBackgroundTick": 1,
    "hasTestedPositiveBackgroundTick": 1,
    "isIsolatingForSelfDiagnosedBackgroundTick": 1,
    "isIsolatingForTestedPositiveBackgroundTick": 1,
    "isIsolatingForHadRiskyContactBackgroundTick": 1
  },
  "includesMultipleApplicationVersions" : false
}
```

#### Validation
* date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
* Apart from the exception list below, all fields are mandatory and must not be null
* Optional metadata:
  * `localAuthority`
* Nullable values:
  * `cumulativeDownloadBytes`
  * `cumulativeUploadBytes`
* Optional values:
  * `receivedVoidTestResultEnteredManually`
  * `receivedPositiveTestResultEnteredManually`
  * `receivedNegativeTestResultEnteredManually`
  * `receivedVoidTestResultViaPolling`
  * `receivedPositiveTestResultViaPolling`
  * `receivedNegativeTestResultViaPolling`
  * `hasSelfDiagnosedBackgroundTick`
  * `hasTestedPositiveBackgroundTick`
  * `isIsolatingForSelfDiagnosedBackgroundTick`
  * `isIsolatingForTestedPositiveBackgroundTick`
  * `isIsolatingForHadRiskyContactBackgroundTick`

### Responses
| Status Code | Description |
| --- | --- |
| 200 | Submission processed |
| 400 | Bad request could not process payload |
