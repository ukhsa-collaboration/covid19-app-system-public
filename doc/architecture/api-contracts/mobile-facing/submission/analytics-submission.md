# Daily Analytics Submission

> API Pattern: [Submission](../../../api-patterns.md#submission)

## HTTP Request and Response

- Submit Mobile Analytics: ```POST https://<FQDN>/submission/mobile-analytics```

### Parameters

- FQDN: Target-environment specific CNAME of the Mobile Submission CloudFront distribution 
- Authorization required and signatures NOT provided - see [API security](../../../api-security.md)
- Extensibility: new Key-Value Pairs can be added to the request without breaking request processing

## Scenario

Mobile clients collect and send app analytics periodically (roughly once per 24h) to the backend.
 
### Submit Mobile Analytics

#### iOS Request Payload Example
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
    "appIsContactTraceableBackgroundTick": 1,
    "appIsUsableBackgroundTick": 1,
    "appIsUsableBluetoothOffBackgroundTick": 1,
    "askedToShareExposureKeysInTheInitialFlow": 1,
    "consentedToShareExposureKeysInReminderScreen": 1,
    "consentedToShareExposureKeysInTheInitialFlow": 1,
    "cumulativeCellularDownloadBytes": 1,
    "cumulativeCellularUploadBytes": 1,
    "cumulativeDownloadBytes": 1,
    "cumulativeUploadBytes": 1,
    "cumulativeWifiDownloadBytes": 1,
    "cumulativeWifiUploadBytes": 1,
    "didAccessLocalInfoScreenViaBanner": 1,
    "didAccessLocalInfoScreenViaNotification": 1,
    "didAskForSymptomsOnPositiveTestEntry": 1,
    "didHaveSymptomsBeforeReceivedTestResult": 1,
    "didRememberOnsetSymptomsDateBeforeReceivedTestResult": 1,
    "didSendLocalInfoNotification": 1,
    "hasRiskyContactNotificationsEnabledBackgroundTick": 1,
    "hasSelfDiagnosedBackgroundTick": 1,
    "hasTestedLFDPositiveBackgroundTick": 1,
    "hasTestedPositiveBackgroundTick": 1,
    "hasTestedSelfRapidPositiveBackgroundTick": 1,
    "haveActiveIpcTokenBackgroundTick": 1,
    "isDisplayingLocalInfoBackgroundTick": 1,
    "isIsolatingForSelfDiagnosedBackgroundTick": 1,
    "isIsolatingForTestedLFDPositiveBackgroundTick": 1,
    "isIsolatingForTestedPositiveBackgroundTick": 1,
    "isIsolatingForTestedSelfRapidPositiveBackgroundTick": 1,
    "isIsolatingForUnconfirmedTestBackgroundTick": 1,
    "launchedIsolationPaymentsApplication": 1,
    "launchedTestOrdering": 1,
    "missingPacketsLast7Days": 1,
    "negativeLabResultAfterPositiveLFDOutsideTimeLimit": 1,
    "negativeLabResultAfterPositiveLFDWithinTimeLimit": 1,
    "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit": 1,
    "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit": 1,
    "optedOutForContactIsolation": 1,
    "optedOutForContactIsolationBackgroundTick": 1,
    "positiveLabResultAfterPositiveLFD": 1,
    "positiveLabResultAfterPositiveSelfRapidTest": 1,
    "receivedActiveIpcToken": 1,
    "receivedNegativeTestResultEnteredManually": 1,
    "receivedNegativeTestResultViaPolling": 1,
    "receivedPositiveLFDTestResultEnteredManually": 1,
    "receivedPositiveSelfRapidTestResultEnteredManually": 1,
    "receivedPositiveTestResultEnteredManually": 1,
    "receivedPositiveTestResultViaPolling": 1,
    "receivedRiskyContactNotification": 1,
    "receivedUnconfirmedPositiveTestResult": 1,
    "receivedVoidTestResultEnteredManually": 1,
    "receivedVoidTestResultViaPolling": 1,
    "selectedIsolationPaymentsButton": 1,
    "startedIsolation": 1,
    "successfullySharedExposureKeys": 1,
    "totalAlarmManagerBackgroundTasks": 1,
    "totalExposureWindowsConsideredRisky": 1,
    "totalExposureWindowsNotConsideredRisky": 1,
    "totalRiskyContactReminderNotifications": 1,
    "totalShareExposureKeysReminderNotifications": 1
  },
  "includesMultipleApplicationVersions" : false
}
```

#### Android Request Payload Example
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
    "appIsContactTraceableBackgroundTick": 1,
    "appIsUsableBackgroundTick": 1,
    "appIsUsableBluetoothOffBackgroundTick": 1,
    "askedToShareExposureKeysInTheInitialFlow": 1,
    "consentedToShareExposureKeysInReminderScreen": 1,
    "consentedToShareExposureKeysInTheInitialFlow": 1,
    "cumulativeDownloadBytes": 1,
    "cumulativeUploadBytes": 1,
    "didAccessLocalInfoScreenViaBanner": 1,
    "didAccessLocalInfoScreenViaNotification": 1,
    "didAskForSymptomsOnPositiveTestEntry": 1,
    "didHaveSymptomsBeforeReceivedTestResult": 1,
    "didRememberOnsetSymptomsDateBeforeReceivedTestResult": 1,
    "didSendLocalInfoNotification": 1,
    "hasRiskyContactNotificationsEnabledBackgroundTick": 1,
    "hasSelfDiagnosedBackgroundTick": 1,
    "hasTestedLFDPositiveBackgroundTick": 1,
    "hasTestedPositiveBackgroundTick": 1,
    "hasTestedSelfRapidPositiveBackgroundTick": 1,
    "haveActiveIpcTokenBackgroundTick": 1,
    "isDisplayingLocalInfoBackgroundTick": 1,
    "isIsolatingForSelfDiagnosedBackgroundTick": 1,
    "isIsolatingForTestedLFDPositiveBackgroundTick": 1,
    "isIsolatingForTestedPositiveBackgroundTick": 1,
    "isIsolatingForTestedSelfRapidPositiveBackgroundTick": 1,
    "isIsolatingForUnconfirmedTestBackgroundTick": 1,
    "launchedIsolationPaymentsApplication": 1,
    "launchedTestOrdering": 1,
    "missingPacketsLast7Days": 1,
    "negativeLabResultAfterPositiveLFDOutsideTimeLimit": 1,
    "negativeLabResultAfterPositiveLFDWithinTimeLimit": 1,
    "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit": 1,
    "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit": 1,
    "optedOutForContactIsolation": 1,
    "optedOutForContactIsolationBackgroundTick": 1,
    "positiveLabResultAfterPositiveLFD": 1,
    "positiveLabResultAfterPositiveSelfRapidTest": 1,
    "receivedActiveIpcToken": 1,
    "receivedNegativeTestResultEnteredManually": 1,
    "receivedNegativeTestResultViaPolling": 1,
    "receivedPositiveLFDTestResultEnteredManually": 1,
    "receivedPositiveSelfRapidTestResultEnteredManually": 1,
    "receivedPositiveTestResultEnteredManually": 1,
    "receivedPositiveTestResultViaPolling": 1,
    "receivedRiskyContactNotification": 1,
    "receivedUnconfirmedPositiveTestResult": 1,
    "receivedVoidTestResultEnteredManually": 1,
    "receivedVoidTestResultViaPolling": 1,
    "selectedIsolationPaymentsButton": 1,
    "startedIsolation": 1,
    "successfullySharedExposureKeys": 1,
    "totalAlarmManagerBackgroundTasks": 1,
    "totalExposureWindowsConsideredRisky": 1,
    "totalExposureWindowsNotConsideredRisky": 1,
    "totalRiskyContactReminderNotifications": 1,
    "totalShareExposureKeysReminderNotifications": 1
  },
  "includesMultipleApplicationVersions" : false
}
```

#### Validation
* date & time in ISO-8601 YYYY-MM-DD'T'hh:mm:ssZ format
* All fields under `metadata` and `analyticsWindow` are **mandatory except** the following:
  * `localAuthority`
* All fields under `metrics` are **optional except** the following:
  * `completedOnboarding`
  * `completedQuestionnaireAndStartedIsolation`
  * `completedQuestionnaireButDidNotStartIsolation`
  * `encounterDetectionPausedBackgroundTick`
  * `hasHadRiskyContactBackgroundTick`
  * `isIsolatingBackgroundTick`
  * `receivedNegativeTestResult`
  * `receivedPositiveTestResult`
  * `receivedVoidTestResult`
  * `runningNormallyBackgroundTick`
  * `totalBackgroundTasks`
* The following fields are optional, but are only expected from iOS devices
  * `cumulativeCellularDownloadBytes`
  * `cumulativeCellularUploadBytes`
  * `cumulativeWifiDownloadBytes`
  * `cumulativeWifiUploadBytes`


### HTTP Response Codes
| Status Code | Description |
| --- | --- |
| `200 OK` | Submission processed |
| `400 Bad Request` | Bad request could not process payload |

##Notes

To preserve user privacy,  smaller postal districts in analytics submissions are aggregated before persisting and forwarding.
See [Privacy Filtering](../../../../design/details/privacy-filtering.md).
