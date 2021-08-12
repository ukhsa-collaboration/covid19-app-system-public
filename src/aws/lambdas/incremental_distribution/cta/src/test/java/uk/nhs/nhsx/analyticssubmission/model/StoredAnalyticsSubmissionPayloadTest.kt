package uk.nhs.nhsx.analyticssubmission.model

import org.junit.jupiter.api.Test
import smoke.data.AnalyticsMetricsData.populatedAnalyticsMetrics
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.events.RecordingEvents
import java.time.Instant

class StoredAnalyticsSubmissionPayloadTest {

    private val startDate = Instant.now()
    private val endDate = Instant.now()

    @Test
    fun `flattens all fields from client payload`() {
        val metrics = populatedAnalyticsMetrics()
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "AL2",
                deviceModel = "some-device-model",
                operatingSystemVersion = "some-os-version",
                latestApplicationVersion = "some-app-version",
                localAuthority = "E07000098"
            ),
            metrics = metrics,
            includesMultipleApplicationVersions = true
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        var counter = 0L
        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "AL2_AL4_WD7",
                deviceModel = "some-device-model",
                operatingSystemVersion = "some-os-version",
                latestApplicationVersion = "some-app-version",
                localAuthority = "E07000098",
                includesMultipleApplicationVersions = true,
                cumulativeDownloadBytes = counter++,
                cumulativeUploadBytes = counter++,
                cumulativeCellularDownloadBytes = counter++,
                cumulativeCellularUploadBytes = counter++,
                cumulativeWifiDownloadBytes = counter++,
                cumulativeWifiUploadBytes = counter++,
                checkedIn = counter++.toInt(),
                canceledCheckIn = counter++.toInt(),
                receivedVoidTestResult = counter++.toInt(),
                isIsolatingBackgroundTick = counter++.toInt(),
                hasHadRiskyContactBackgroundTick = counter++.toInt(),
                receivedPositiveTestResult = counter++.toInt(),
                receivedNegativeTestResult = counter++.toInt(),
                hasSelfDiagnosedPositiveBackgroundTick = counter++.toInt(),
                completedQuestionnaireAndStartedIsolation = counter++.toInt(),
                encounterDetectionPausedBackgroundTick = counter++.toInt(),
                completedQuestionnaireButDidNotStartIsolation = counter++.toInt(),
                totalBackgroundTasks = counter++.toInt(),
                runningNormallyBackgroundTick = counter++.toInt(),
                completedOnboarding = counter++.toInt(),
                receivedVoidTestResultEnteredManually = counter++.toInt(),
                receivedPositiveTestResultEnteredManually = counter++.toInt(),
                receivedNegativeTestResultEnteredManually = counter++.toInt(),
                receivedVoidTestResultViaPolling = counter++.toInt(),
                receivedPositiveTestResultViaPolling = counter++.toInt(),
                receivedNegativeTestResultViaPolling = counter++.toInt(),
                hasSelfDiagnosedBackgroundTick = counter++.toInt(),
                hasTestedPositiveBackgroundTick = counter++.toInt(),
                isIsolatingForSelfDiagnosedBackgroundTick = counter++.toInt(),
                isIsolatingForTestedPositiveBackgroundTick = counter++.toInt(),
                isIsolatingForHadRiskyContactBackgroundTick = counter++.toInt(),
                receivedRiskyContactNotification = counter++.toInt(),
                startedIsolation = counter++.toInt(),
                receivedPositiveTestResultWhenIsolatingDueToRiskyContact = counter++.toInt(),
                receivedActiveIpcToken = counter++.toInt(),
                haveActiveIpcTokenBackgroundTick = counter++.toInt(),
                selectedIsolationPaymentsButton = counter++.toInt(),
                launchedIsolationPaymentsApplication = counter++.toInt(),
                receivedPositiveLFDTestResultViaPolling = counter++.toInt(),
                receivedNegativeLFDTestResultViaPolling = counter++.toInt(),
                receivedVoidLFDTestResultViaPolling = counter++.toInt(),
                receivedPositiveLFDTestResultEnteredManually = counter++.toInt(),
                receivedNegativeLFDTestResultEnteredManually = counter++.toInt(),
                receivedVoidLFDTestResultEnteredManually = counter++.toInt(),
                hasTestedLFDPositiveBackgroundTick = counter++.toInt(),
                isIsolatingForTestedLFDPositiveBackgroundTick = counter++.toInt(),
                totalExposureWindowsNotConsideredRisky = counter++.toInt(),
                totalExposureWindowsConsideredRisky = counter++.toInt(),
                acknowledgedStartOfIsolationDueToRiskyContact = counter++.toInt(),
                hasRiskyContactNotificationsEnabledBackgroundTick = counter++.toInt(),
                totalRiskyContactReminderNotifications = counter++.toInt(),
                receivedUnconfirmedPositiveTestResult = counter++.toInt(),
                isIsolatingForUnconfirmedTestBackgroundTick = counter++.toInt(),
                launchedTestOrdering = counter++.toInt(),
                didHaveSymptomsBeforeReceivedTestResult = counter++.toInt(),
                didRememberOnsetSymptomsDateBeforeReceivedTestResult = counter++.toInt(),
                didAskForSymptomsOnPositiveTestEntry = counter++.toInt(),
                declaredNegativeResultFromDCT = counter++.toInt(),
                receivedPositiveSelfRapidTestResultViaPolling = counter++.toInt(),
                receivedNegativeSelfRapidTestResultViaPolling = counter++.toInt(),
                receivedVoidSelfRapidTestResultViaPolling = counter++.toInt(),
                receivedPositiveSelfRapidTestResultEnteredManually = counter++.toInt(),
                receivedNegativeSelfRapidTestResultEnteredManually = counter++.toInt(),
                receivedVoidSelfRapidTestResultEnteredManually = counter++.toInt(),
                isIsolatingForTestedSelfRapidPositiveBackgroundTick = counter++.toInt(),
                hasTestedSelfRapidPositiveBackgroundTick = counter++.toInt(),
                receivedRiskyVenueM1Warning = counter++.toInt(),
                receivedRiskyVenueM2Warning = counter++.toInt(),
                hasReceivedRiskyVenueM2WarningBackgroundTick = counter++.toInt(),
                totalAlarmManagerBackgroundTasks = counter++.toInt(),
                missingPacketsLast7Days = counter++.toInt(),
                consentedToShareVenueHistory = counter++.toInt(),
                askedToShareVenueHistory = counter++.toInt(),
                askedToShareExposureKeysInTheInitialFlow = counter++.toInt(),
                consentedToShareExposureKeysInTheInitialFlow = counter++.toInt(),
                totalShareExposureKeysReminderNotifications = counter++.toInt(),
                consentedToShareExposureKeysInReminderScreen = counter++.toInt(),
                successfullySharedExposureKeys = counter++.toInt(),
                didSendLocalInfoNotification = counter++.toInt(),
                didAccessLocalInfoScreenViaNotification = counter++.toInt(),
                didAccessLocalInfoScreenViaBanner = counter++.toInt(),
                isDisplayingLocalInfoBackgroundTick = counter++.toInt(),
                positiveLabResultAfterPositiveLFD = counter++.toInt(),
                negativeLabResultAfterPositiveLFDWithinTimeLimit = counter++.toInt(),
                negativeLabResultAfterPositiveLFDOutsideTimeLimit = counter++.toInt(),
                positiveLabResultAfterPositiveSelfRapidTest = counter++.toInt(),
                negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit = counter++.toInt(),
                negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit = counter++.toInt(),
                didAccessRiskyVenueM2Notification = counter++.toInt(),
                selectedTakeTestM2Journey = counter++.toInt(),
                selectedTakeTestLaterM2Journey = counter++.toInt(),
                selectedHasSymptomsM2Journey = counter++.toInt(),
                selectedHasNoSymptomsM2Journey = counter++.toInt(),
                selectedLFDTestOrderingM2Journey = counter++.toInt(),
                selectedHasLFDTestM2Journey = counter++.toInt(),
                optedOutForContactIsolation = counter++.toInt(),
                optedOutForContactIsolationBackgroundTick = counter.toInt(),
            )
        )
    }

    @Test
    fun `merged postal district with matching local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "AL2",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = "E07000098"
            ),
            metrics = AnalyticsMetrics(),
            includesMultipleApplicationVersions = false
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "AL2_AL4_WD7",
                localAuthority = "E07000098",
                includesMultipleApplicationVersions = false
            )
        )
    }

    @Test
    fun `merged postal district with null local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "AB13",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = null
            ),
            metrics = AnalyticsMetrics(),
            includesMultipleApplicationVersions = false
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "AB13_AB14",
                localAuthority = null,
                includesMultipleApplicationVersions = false
            )
        )
    }

    @Test
    fun `merged postal district with non-matching local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "YO62",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = "E07000152"
            ),
            metrics = AnalyticsMetrics(),
            includesMultipleApplicationVersions = false
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "YO60_YO62",
                localAuthority = null,
                includesMultipleApplicationVersions = false
            )
        )
    }

    @Test
    fun `merged postal district with invalid local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "YO62",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = "Houston"
            ),
            metrics = AnalyticsMetrics(),
            includesMultipleApplicationVersions = false
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "YO60_YO62",
                localAuthority = null,
                includesMultipleApplicationVersions = false
            )
        )
    }

    @Test
    fun `empty postal district and empty local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = ""
            ),
            metrics = AnalyticsMetrics(),
            includesMultipleApplicationVersions = false
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "NOT SET",
                localAuthority = null,
                includesMultipleApplicationVersions = false
            )
        )
    }

    @Test
    fun `unknown postcode with non-null local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "F4KEP0STC0DE",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = "E06000051"
            ),
            metrics = AnalyticsMetrics(),
            includesMultipleApplicationVersions = false
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "UNKNOWN",
                localAuthority = "UNKNOWN",
                includesMultipleApplicationVersions = false
            )
        )
    }

    @Test
    fun `unknown postcode with null local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(startDate, endDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "F4KEP0STC0DE",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = null
            ),
            metrics = AnalyticsMetrics(),
            includesMultipleApplicationVersions = false
        )

        val storedPayload = StoredAnalyticsSubmissionPayload.convertFrom(clientPayload, RecordingEvents())

        expectThat(storedPayload).isEqualTo(
            expectedStoredAnalytics(
                postalDistrict = "UNKNOWN",
                localAuthority = "UNKNOWN",
                includesMultipleApplicationVersions = false
            )
        )
    }

    private fun expectedStoredAnalytics(
        eventStartDate: Instant = startDate,
        eventEndDate: Instant = endDate,
        postalDistrict: String,
        deviceModel: String = "",
        operatingSystemVersion: String = "",
        latestApplicationVersion: String = "",
        localAuthority: String?,
        cumulativeDownloadBytes: Long? = null,
        cumulativeUploadBytes: Long? = null,
        cumulativeCellularDownloadBytes: Long? = null,
        cumulativeCellularUploadBytes: Long? = null,
        cumulativeWifiDownloadBytes: Long? = null,
        cumulativeWifiUploadBytes: Long? = null,
        checkedIn: Int = 0,
        canceledCheckIn: Int = 0,
        receivedVoidTestResult: Int = 0,
        isIsolatingBackgroundTick: Int = 0,
        hasHadRiskyContactBackgroundTick: Int = 0,
        receivedPositiveTestResult: Int = 0,
        receivedNegativeTestResult: Int = 0,
        hasSelfDiagnosedPositiveBackgroundTick: Int = 0,
        completedQuestionnaireAndStartedIsolation: Int = 0,
        encounterDetectionPausedBackgroundTick: Int = 0,
        completedQuestionnaireButDidNotStartIsolation: Int = 0,
        totalBackgroundTasks: Int = 0,
        runningNormallyBackgroundTick: Int = 0,
        completedOnboarding: Int = 0,
        includesMultipleApplicationVersions: Boolean,
        receivedVoidTestResultEnteredManually: Int? = null,
        receivedPositiveTestResultEnteredManually: Int? = null,
        receivedNegativeTestResultEnteredManually: Int? = null,
        receivedVoidTestResultViaPolling: Int? = null,
        receivedPositiveTestResultViaPolling: Int? = null,
        receivedNegativeTestResultViaPolling: Int? = null,
        hasSelfDiagnosedBackgroundTick: Int? = null,
        hasTestedPositiveBackgroundTick: Int? = null,
        isIsolatingForSelfDiagnosedBackgroundTick: Int? = null,
        isIsolatingForTestedPositiveBackgroundTick: Int? = null,
        isIsolatingForHadRiskyContactBackgroundTick: Int? = null,
        receivedRiskyContactNotification: Int? = null,
        startedIsolation: Int? = null,
        receivedPositiveTestResultWhenIsolatingDueToRiskyContact: Int? = null,
        receivedActiveIpcToken: Int? = null,
        haveActiveIpcTokenBackgroundTick: Int? = null,
        selectedIsolationPaymentsButton: Int? = null,
        launchedIsolationPaymentsApplication: Int? = null,
        receivedPositiveLFDTestResultViaPolling: Int? = null,
        receivedNegativeLFDTestResultViaPolling: Int? = null,
        receivedVoidLFDTestResultViaPolling: Int? = null,
        receivedPositiveLFDTestResultEnteredManually: Int? = null,
        receivedNegativeLFDTestResultEnteredManually: Int? = null,
        receivedVoidLFDTestResultEnteredManually: Int? = null,
        hasTestedLFDPositiveBackgroundTick: Int? = null,
        isIsolatingForTestedLFDPositiveBackgroundTick: Int? = null,
        totalExposureWindowsNotConsideredRisky: Int? = null,
        totalExposureWindowsConsideredRisky: Int? = null,
        acknowledgedStartOfIsolationDueToRiskyContact: Int? = null,
        hasRiskyContactNotificationsEnabledBackgroundTick: Int? = null,
        totalRiskyContactReminderNotifications: Int? = null,
        receivedUnconfirmedPositiveTestResult: Int? = null,
        isIsolatingForUnconfirmedTestBackgroundTick: Int? = null,
        launchedTestOrdering: Int? = null,
        didHaveSymptomsBeforeReceivedTestResult: Int? = null,
        didRememberOnsetSymptomsDateBeforeReceivedTestResult: Int? = null,
        didAskForSymptomsOnPositiveTestEntry: Int? = null,
        declaredNegativeResultFromDCT: Int? = null,
        receivedPositiveSelfRapidTestResultViaPolling: Int? = null,
        receivedNegativeSelfRapidTestResultViaPolling: Int? = null,
        receivedVoidSelfRapidTestResultViaPolling: Int? = null,
        receivedPositiveSelfRapidTestResultEnteredManually: Int? = null,
        receivedNegativeSelfRapidTestResultEnteredManually: Int? = null,
        receivedVoidSelfRapidTestResultEnteredManually: Int? = null,
        isIsolatingForTestedSelfRapidPositiveBackgroundTick: Int? = null,
        hasTestedSelfRapidPositiveBackgroundTick: Int? = null,
        receivedRiskyVenueM1Warning: Int? = null,
        receivedRiskyVenueM2Warning: Int? = null,
        hasReceivedRiskyVenueM2WarningBackgroundTick: Int? = null,
        totalAlarmManagerBackgroundTasks: Int? = null,
        missingPacketsLast7Days: Int? = null,
        consentedToShareVenueHistory: Int? = null,
        askedToShareVenueHistory: Int? = null,
        askedToShareExposureKeysInTheInitialFlow: Int? = null,
        consentedToShareExposureKeysInTheInitialFlow: Int? = null,
        totalShareExposureKeysReminderNotifications: Int? = null,
        consentedToShareExposureKeysInReminderScreen: Int? = null,
        successfullySharedExposureKeys: Int? = null,
        didSendLocalInfoNotification: Int? = null,
        didAccessLocalInfoScreenViaNotification: Int? = null,
        didAccessLocalInfoScreenViaBanner: Int? = null,
        isDisplayingLocalInfoBackgroundTick: Int? = null,
        positiveLabResultAfterPositiveLFD: Int? = null,
        negativeLabResultAfterPositiveLFDWithinTimeLimit: Int? = null,
        negativeLabResultAfterPositiveLFDOutsideTimeLimit: Int? = null,
        positiveLabResultAfterPositiveSelfRapidTest: Int? = null,
        negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit: Int? = null,
        negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit: Int? = null,
        didAccessRiskyVenueM2Notification: Int? = null,
        selectedTakeTestM2Journey: Int? = null,
        selectedTakeTestLaterM2Journey: Int? = null,
        selectedHasSymptomsM2Journey: Int? = null,
        selectedHasNoSymptomsM2Journey: Int? = null,
        selectedLFDTestOrderingM2Journey: Int? = null,
        selectedHasLFDTestM2Journey: Int? = null,
        optedOutForContactIsolation: Int? = null,
        optedOutForContactIsolationBackgroundTick: Int? = null,
    ) = mapOf(
        "startDate" to "$eventStartDate",
        "endDate" to "$eventEndDate",
        "postalDistrict" to postalDistrict,
        "deviceModel" to deviceModel,
        "operatingSystemVersion" to operatingSystemVersion,
        "latestApplicationVersion" to latestApplicationVersion,
        "localAuthority" to localAuthority,
        "cumulativeDownloadBytes" to cumulativeDownloadBytes,
        "cumulativeUploadBytes" to cumulativeUploadBytes,
        "cumulativeCellularDownloadBytes" to cumulativeCellularDownloadBytes,
        "cumulativeCellularUploadBytes" to cumulativeCellularUploadBytes,
        "cumulativeWifiDownloadBytes" to cumulativeWifiDownloadBytes,
        "cumulativeWifiUploadBytes" to cumulativeWifiUploadBytes,
        "checkedIn" to checkedIn,
        "canceledCheckIn" to canceledCheckIn,
        "receivedVoidTestResult" to receivedVoidTestResult,
        "isIsolatingBackgroundTick" to isIsolatingBackgroundTick,
        "hasHadRiskyContactBackgroundTick" to hasHadRiskyContactBackgroundTick,
        "receivedPositiveTestResult" to receivedPositiveTestResult,
        "receivedNegativeTestResult" to receivedNegativeTestResult,
        "hasSelfDiagnosedPositiveBackgroundTick" to hasSelfDiagnosedPositiveBackgroundTick,
        "completedQuestionnaireAndStartedIsolation" to completedQuestionnaireAndStartedIsolation,
        "encounterDetectionPausedBackgroundTick" to encounterDetectionPausedBackgroundTick,
        "completedQuestionnaireButDidNotStartIsolation" to completedQuestionnaireButDidNotStartIsolation,
        "totalBackgroundTasks" to totalBackgroundTasks,
        "runningNormallyBackgroundTick" to runningNormallyBackgroundTick,
        "completedOnboarding" to completedOnboarding,
        "includesMultipleApplicationVersions" to includesMultipleApplicationVersions,
        "receivedVoidTestResultEnteredManually" to receivedVoidTestResultEnteredManually,
        "receivedPositiveTestResultEnteredManually" to receivedPositiveTestResultEnteredManually,
        "receivedNegativeTestResultEnteredManually" to receivedNegativeTestResultEnteredManually,
        "receivedVoidTestResultViaPolling" to receivedVoidTestResultViaPolling,
        "receivedPositiveTestResultViaPolling" to receivedPositiveTestResultViaPolling,
        "receivedNegativeTestResultViaPolling" to receivedNegativeTestResultViaPolling,
        "hasSelfDiagnosedBackgroundTick" to hasSelfDiagnosedBackgroundTick,
        "hasTestedPositiveBackgroundTick" to hasTestedPositiveBackgroundTick,
        "isIsolatingForSelfDiagnosedBackgroundTick" to isIsolatingForSelfDiagnosedBackgroundTick,
        "isIsolatingForTestedPositiveBackgroundTick" to isIsolatingForTestedPositiveBackgroundTick,
        "isIsolatingForHadRiskyContactBackgroundTick" to isIsolatingForHadRiskyContactBackgroundTick,
        "receivedRiskyContactNotification" to receivedRiskyContactNotification,
        "startedIsolation" to startedIsolation,
        "receivedPositiveTestResultWhenIsolatingDueToRiskyContact" to receivedPositiveTestResultWhenIsolatingDueToRiskyContact,
        "receivedActiveIpcToken" to receivedActiveIpcToken,
        "haveActiveIpcTokenBackgroundTick" to haveActiveIpcTokenBackgroundTick,
        "selectedIsolationPaymentsButton" to selectedIsolationPaymentsButton,
        "launchedIsolationPaymentsApplication" to launchedIsolationPaymentsApplication,
        "receivedPositiveLFDTestResultViaPolling" to receivedPositiveLFDTestResultViaPolling,
        "receivedNegativeLFDTestResultViaPolling" to receivedNegativeLFDTestResultViaPolling,
        "receivedVoidLFDTestResultViaPolling" to receivedVoidLFDTestResultViaPolling,
        "receivedPositiveLFDTestResultEnteredManually" to receivedPositiveLFDTestResultEnteredManually,
        "receivedNegativeLFDTestResultEnteredManually" to receivedNegativeLFDTestResultEnteredManually,
        "receivedVoidLFDTestResultEnteredManually" to receivedVoidLFDTestResultEnteredManually,
        "hasTestedLFDPositiveBackgroundTick" to hasTestedLFDPositiveBackgroundTick,
        "isIsolatingForTestedLFDPositiveBackgroundTick" to isIsolatingForTestedLFDPositiveBackgroundTick,
        "totalExposureWindowsNotConsideredRisky" to totalExposureWindowsNotConsideredRisky,
        "totalExposureWindowsConsideredRisky" to totalExposureWindowsConsideredRisky,
        "acknowledgedStartOfIsolationDueToRiskyContact" to acknowledgedStartOfIsolationDueToRiskyContact,
        "hasRiskyContactNotificationsEnabledBackgroundTick" to hasRiskyContactNotificationsEnabledBackgroundTick,
        "totalRiskyContactReminderNotifications" to totalRiskyContactReminderNotifications,
        "receivedUnconfirmedPositiveTestResult" to receivedUnconfirmedPositiveTestResult,
        "isIsolatingForUnconfirmedTestBackgroundTick" to isIsolatingForUnconfirmedTestBackgroundTick,
        "launchedTestOrdering" to launchedTestOrdering,
        "didHaveSymptomsBeforeReceivedTestResult" to didHaveSymptomsBeforeReceivedTestResult,
        "didRememberOnsetSymptomsDateBeforeReceivedTestResult" to didRememberOnsetSymptomsDateBeforeReceivedTestResult,
        "didAskForSymptomsOnPositiveTestEntry" to didAskForSymptomsOnPositiveTestEntry,
        "declaredNegativeResultFromDCT" to declaredNegativeResultFromDCT,
        "receivedPositiveSelfRapidTestResultViaPolling" to receivedPositiveSelfRapidTestResultViaPolling,
        "receivedNegativeSelfRapidTestResultViaPolling" to receivedNegativeSelfRapidTestResultViaPolling,
        "receivedVoidSelfRapidTestResultViaPolling" to receivedVoidSelfRapidTestResultViaPolling,
        "receivedPositiveSelfRapidTestResultEnteredManually" to receivedPositiveSelfRapidTestResultEnteredManually,
        "receivedNegativeSelfRapidTestResultEnteredManually" to receivedNegativeSelfRapidTestResultEnteredManually,
        "receivedVoidSelfRapidTestResultEnteredManually" to receivedVoidSelfRapidTestResultEnteredManually,
        "isIsolatingForTestedSelfRapidPositiveBackgroundTick" to isIsolatingForTestedSelfRapidPositiveBackgroundTick,
        "hasTestedSelfRapidPositiveBackgroundTick" to hasTestedSelfRapidPositiveBackgroundTick,
        "receivedRiskyVenueM1Warning" to receivedRiskyVenueM1Warning,
        "receivedRiskyVenueM2Warning" to receivedRiskyVenueM2Warning,
        "hasReceivedRiskyVenueM2WarningBackgroundTick" to hasReceivedRiskyVenueM2WarningBackgroundTick,
        "totalAlarmManagerBackgroundTasks" to totalAlarmManagerBackgroundTasks,
        "missingPacketsLast7Days" to missingPacketsLast7Days,
        "consentedToShareVenueHistory" to consentedToShareVenueHistory,
        "askedToShareVenueHistory" to askedToShareVenueHistory,
        "askedToShareExposureKeysInTheInitialFlow" to askedToShareExposureKeysInTheInitialFlow,
        "consentedToShareExposureKeysInTheInitialFlow" to consentedToShareExposureKeysInTheInitialFlow,
        "totalShareExposureKeysReminderNotifications" to totalShareExposureKeysReminderNotifications,
        "consentedToShareExposureKeysInReminderScreen" to consentedToShareExposureKeysInReminderScreen,
        "successfullySharedExposureKeys" to successfullySharedExposureKeys,
        "didSendLocalInfoNotification" to didSendLocalInfoNotification,
        "didAccessLocalInfoScreenViaNotification" to didAccessLocalInfoScreenViaNotification,
        "didAccessLocalInfoScreenViaBanner" to didAccessLocalInfoScreenViaBanner,
        "isDisplayingLocalInfoBackgroundTick" to isDisplayingLocalInfoBackgroundTick,
        "positiveLabResultAfterPositiveLFD" to positiveLabResultAfterPositiveLFD,
        "negativeLabResultAfterPositiveLFDWithinTimeLimit" to negativeLabResultAfterPositiveLFDWithinTimeLimit,
        "negativeLabResultAfterPositiveLFDOutsideTimeLimit" to negativeLabResultAfterPositiveLFDOutsideTimeLimit,
        "positiveLabResultAfterPositiveSelfRapidTest" to positiveLabResultAfterPositiveSelfRapidTest,
        "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit" to negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit,
        "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit" to negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit,
        "didAccessRiskyVenueM2Notification" to didAccessRiskyVenueM2Notification,
        "selectedTakeTestM2Journey" to selectedTakeTestM2Journey,
        "selectedTakeTestLaterM2Journey" to selectedTakeTestLaterM2Journey,
        "selectedHasSymptomsM2Journey" to selectedHasSymptomsM2Journey,
        "selectedHasNoSymptomsM2Journey" to selectedHasNoSymptomsM2Journey,
        "selectedLFDTestOrderingM2Journey" to selectedLFDTestOrderingM2Journey,
        "selectedHasLFDTestM2Journey" to selectedHasLFDTestM2Journey,
        "optedOutForContactIsolation" to optedOutForContactIsolation,
        "optedOutForContactIsolationBackgroundTick" to optedOutForContactIsolationBackgroundTick
    )

}
