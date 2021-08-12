package uk.nhs.nhsx.analyticssubmission.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Json
import kotlin.reflect.full.memberProperties

class ClientAnalyticsSubmissionPayloadTest {

    @Test
    fun `can deserialize first version of android payload`() {
        val json = """
            {
                "metadata":{
                    "operatingSystemVersion":"29",
                    "latestApplicationVersion":"3.0",
                    "deviceModel":"HUAWEI LDN-L21",
                    "postalDistrict":"AB10"
                },
                "analyticsWindow":{
                    "endDate":"2020-07-28T22:59:00Z",
                    "startDate":"2020-07-27T23:00:00Z"
                },
                "metrics":{
                    "cumulativeDownloadBytes":123,
                    "cumulativeUploadBytes":456,
                    "checkedIn":1,
                    "canceledCheckIn":1,
                    "receivedVoidTestResult":1,
                    "isIsolatingBackgroundTick":1,
                    "hasHadRiskyContactBackgroundTick":1,
                    "receivedPositiveTestResult":1,
                    "receivedNegativeTestResult":1,
                    "hasSelfDiagnosedPositiveBackgroundTick":1,
                    "completedQuestionnaireAndStartedIsolation":1,
                    "encounterDetectionPausedBackgroundTick":1,
                    "completedQuestionnaireButDidNotStartIsolation":1,
                    "totalBackgroundTasks":1,
                    "runningNormallyBackgroundTick":1,
                    "completedOnboarding":1
                },
                "includesMultipleApplicationVersions":false
            }""".trimIndent()

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("29")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("HUAWEI LDN-L21")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isNull()
        }

        expectThat(payload.metrics) {
            get { cumulativeDownloadBytes }.isEqualTo(123)
            get { cumulativeUploadBytes }.isEqualTo(456)
            get { cumulativeCellularDownloadBytes }.isNull()
            get { cumulativeCellularUploadBytes }.isNull()
            get { cumulativeWifiDownloadBytes }.isNull()
            get { cumulativeWifiUploadBytes }.isNull()
        }
    }

    @Test
    fun `can deserialize first version ios payload`() {
        // iOS has extra fields for cellular and wifi bytes
        val json = """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "AB10"
                },
                "analyticsWindow": {
                    "endDate": "2020-07-28T22:59:00Z",
                    "startDate": "2020-07-27T23:00:00Z"
                },
                "metrics": {
                    "cumulativeDownloadBytes": 140000000,
                    "cumulativeUploadBytes": 140000000,
                    "cumulativeCellularDownloadBytes": 80000000,
                    "cumulativeCellularUploadBytes": 70000000,
                    "cumulativeWifiDownloadBytes": 60000000,
                    "cumulativeWifiUploadBytes": 50000000,
                    "checkedIn": 1,
                    "canceledCheckIn": 1,
                    "receivedVoidTestResult": 1,
                    "isIsolatingBackgroundTick": 1,
                    "hasHadRiskyContactBackgroundTick": 1,
                    "receivedPositiveTestResult": 1,
                    "receivedNegativeTestResult": 1,
                    "hasSelfDiagnosedPositiveBackgroundTick": 1,
                    "completedQuestionnaireAndStartedIsolation": 1,
                    "encounterDetectionPausedBackgroundTick": 1,
                    "completedQuestionnaireButDidNotStartIsolation": 1,
                    "totalBackgroundTasks": 1,
                    "runningNormallyBackgroundTick": 1,
                    "completedOnboarding": 1
                },
                "includesMultipleApplicationVersions": false
            }
        """.trimIndent()

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("iPhone OS 13.5.1 (17F80)")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("iPhone11,2")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isNull()
        }
    }

    @Test
    fun `can deserialize with local authority field`() {
        val json = """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "AB10",
                    "localAuthority": "E06000051"
                },
                "analyticsWindow": {
                    "endDate": "2020-07-28T22:59:00Z",
                    "startDate": "2020-07-27T23:00:00Z"
                },
                "metrics": {
                    "cumulativeDownloadBytes": 140000000,
                    "cumulativeUploadBytes": 140000000,
                    "cumulativeCellularDownloadBytes": 80000000,
                    "cumulativeCellularUploadBytes": 70000000,
                    "cumulativeWifiDownloadBytes": 60000000,
                    "cumulativeWifiUploadBytes": 50000000,
                    "checkedIn": 1,
                    "canceledCheckIn": 1,
                    "receivedVoidTestResult": 1,
                    "isIsolatingBackgroundTick": 1,
                    "hasHadRiskyContactBackgroundTick": 1,
                    "receivedPositiveTestResult": 1,
                    "receivedNegativeTestResult": 1,
                    "hasSelfDiagnosedPositiveBackgroundTick": 1,
                    "completedQuestionnaireAndStartedIsolation": 1,
                    "encounterDetectionPausedBackgroundTick": 1,
                    "completedQuestionnaireButDidNotStartIsolation": 1,
                    "totalBackgroundTasks": 1,
                    "runningNormallyBackgroundTick": 1,
                    "completedOnboarding": 1
                },
                "includesMultipleApplicationVersions": false
            }
        """.trimIndent()

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("iPhone OS 13.5.1 (17F80)")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("iPhone11,2")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isEqualTo("E06000051")
        }
    }

    @Test
    fun `can deserialize with all optional metrics in newer versions of payload`() {
        val json = """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "AB10",
                    "localAuthority": "E06000051"
                },
                "analyticsWindow": {
                    "endDate": "2020-07-28T22:59:00Z",
                    "startDate": "2020-07-27T23:00:00Z"
                },
                "metrics": {
                    "cumulativeDownloadBytes": 140000000,
                    "cumulativeUploadBytes": 140000000,
                    "cumulativeCellularDownloadBytes": 80000000,
                    "cumulativeCellularUploadBytes": 70000000,
                    "cumulativeWifiDownloadBytes": 60000000,
                    "cumulativeWifiUploadBytes": 50000000,
                    "checkedIn": 1,
                    "canceledCheckIn": 1,
                    "receivedVoidTestResult": 1,
                    "isIsolatingBackgroundTick": 1,
                    "hasHadRiskyContactBackgroundTick": 1,
                    "receivedPositiveTestResult": 1,
                    "receivedNegativeTestResult": 1,
                    "hasSelfDiagnosedPositiveBackgroundTick": 1,
                    "completedQuestionnaireAndStartedIsolation": 1,
                    "encounterDetectionPausedBackgroundTick": 1,
                    "completedQuestionnaireButDidNotStartIsolation": 1,
                    "totalBackgroundTasks": 1,
                    "runningNormallyBackgroundTick": 1,
                    "completedOnboarding": 1,
                    "receivedVoidTestResultEnteredManually" : 1,    
                    "receivedPositiveTestResultEnteredManually" : 1,    
                    "receivedNegativeTestResultEnteredManually" : 1,    
                    "receivedVoidTestResultViaPolling" : 1,    
                    "receivedPositiveTestResultViaPolling" : 1,    
                    "receivedNegativeTestResultViaPolling" : 1,    
                    "hasSelfDiagnosedBackgroundTick" : 4,    
                    "hasTestedPositiveBackgroundTick" : 5,    
                    "isIsolatingForSelfDiagnosedBackgroundTick" : 6,    
                    "isIsolatingForTestedPositiveBackgroundTick" : 3,    
                    "isIsolatingForHadRiskyContactBackgroundTick" : 13,    
                    "receivedRiskyContactNotification" : 1,    
                    "startedIsolation" : 1,    
                    "receivedPositiveTestResultWhenIsolatingDueToRiskyContact" : 1,
                    "receivedActiveIpcToken": 1,
                    "haveActiveIpcTokenBackgroundTick": 1,
                    "selectedIsolationPaymentsButton": 1,
                    "launchedIsolationPaymentsApplication": 1,
                    "receivedPositiveLFDTestResultViaPolling":  1,
                    "receivedNegativeLFDTestResultViaPolling":  1,
                    "receivedVoidLFDTestResultViaPolling":  1,
                    "receivedPositiveLFDTestResultEnteredManually": 1,
                    "receivedNegativeLFDTestResultEnteredManually": 1,
                    "receivedVoidLFDTestResultEnteredManually": 1,
                    "hasTestedLFDPositiveBackgroundTick": 1,
                    "isIsolatingForTestedLFDPositiveBackgroundTick": 1,
                    "totalExposureWindowsNotConsideredRisky": 1,
                    "totalExposureWindowsConsideredRisky": 1,
                    "acknowledgedStartOfIsolationDueToRiskyContact":1,
                    "hasRiskyContactNotificationsEnabledBackgroundTick":1,
                    "totalRiskyContactReminderNotifications":1,
                    "receivedUnconfirmedPositiveTestResult":1,
                    "isIsolatingForUnconfirmedTestBackgroundTick":1,
                    "launchedTestOrdering":1,
                    "didHaveSymptomsBeforeReceivedTestResult":1,
                    "didRememberOnsetSymptomsDateBeforeReceivedTestResult":1,
                    "didAskForSymptomsOnPositiveTestEntry":1,
                    "declaredNegativeResultFromDCT":1,
                    "receivedPositiveSelfRapidTestResultViaPolling":1,
                    "receivedNegativeSelfRapidTestResultViaPolling":1,
                    "receivedVoidSelfRapidTestResultViaPolling":1,
                    "receivedPositiveSelfRapidTestResultEnteredManually":1,
                    "receivedNegativeSelfRapidTestResultEnteredManually":1,
                    "receivedVoidSelfRapidTestResultEnteredManually":1,
                    "isIsolatingForTestedSelfRapidPositiveBackgroundTick":1,
                    "hasTestedSelfRapidPositiveBackgroundTick":1,
                    "receivedRiskyVenueM1Warning":1,
                    "receivedRiskyVenueM2Warning":1,
                    "hasReceivedRiskyVenueM2WarningBackgroundTick":1,
                    "totalAlarmManagerBackgroundTasks":1,
                    "missingPacketsLast7Days":1,
                    "consentedToShareVenueHistory":1,
                    "askedToShareVenueHistory":1,
                    "askedToShareExposureKeysInTheInitialFlow":1,
                    "consentedToShareExposureKeysInTheInitialFlow":1,
                    "totalShareExposureKeysReminderNotifications":1,
                    "consentedToShareExposureKeysInReminderScreen":1,
                    "successfullySharedExposureKeys":1,
                    "didSendLocalInfoNotification":1,
                    "didAccessLocalInfoScreenViaNotification":1,
                    "didAccessLocalInfoScreenViaBanner":1,
                    "isDisplayingLocalInfoBackgroundTick":1,
                    "positiveLabResultAfterPositiveLFD":1,
                    "negativeLabResultAfterPositiveLFDWithinTimeLimit":1,
                    "negativeLabResultAfterPositiveLFDOutsideTimeLimit":1,
                    "positiveLabResultAfterPositiveSelfRapidTest":1,
                    "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit":1,
                    "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit":1,
                    "didAccessRiskyVenueM2Notification":1,
                    "selectedTakeTestM2Journey":1,
                    "selectedTakeTestLaterM2Journey":1,
                    "selectedHasSymptomsM2Journey":1,
                    "selectedHasNoSymptomsM2Journey":1,
                    "selectedLFDTestOrderingM2Journey":1,
                    "selectedHasLFDTestM2Journey":1,
                    "optedOutForContactIsolation":1,
                    "optedOutForContactIsolationBackgroundTick":1
                },
                "includesMultipleApplicationVersions": false
            }
        """.trimIndent()

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("iPhone OS 13.5.1 (17F80)")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("iPhone11,2")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isEqualTo("E06000051")
        }

        payload.metrics::class.memberProperties.forEach {
            val propertyValue = it.getter.call(payload.metrics)
                ?: fail("unexpected field [${it.name}], if this is a new field then add it to the unit test payload with non-null value greater than 0")

            when (propertyValue) {
                is Int -> expectThat(propertyValue).isGreaterThan(0)
                is Long -> expectThat(propertyValue).isGreaterThan(0)
                else -> fail("metrics in this class should be numbers, unexpected type [${it.returnType}] for field [${it.name}]")
            }
        }
    }
}
