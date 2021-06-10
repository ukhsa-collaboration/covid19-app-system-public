package smoke

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import smoke.actors.Analytics
import smoke.actors.MobileApp
import smoke.actors.MobileDeviceModel
import smoke.env.SmokeTests
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.MobileOS.Android
import uk.nhs.nhsx.testhelper.http4k.assertApproved
import uk.nhs.nhsx.testhelper.junit.assertWithin
import java.time.Duration
import java.time.Duration.ofDays
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import org.http4k.format.Jackson as Http4kJackson

@ExtendWith(ApprovalTest::class)
class AnalyticsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val startDate = Instant.now().minus(ofDays(1)).truncatedTo(ChronoUnit.SECONDS)
    private val endDate = Instant.now().plus(ofDays(1)).truncatedTo(ChronoUnit.SECONDS)
    private val analytics = Analytics(config, client, CLOCK)

    @Test
    fun `submit ios analytics data`(approver: Approver) {
        val deviceModel = MobileDeviceModel(UUID.randomUUID().toString())
        val mobileApp = MobileApp(client, config, os = MobileOS.iOS, model = deviceModel)
        val metrics = populatedAnalyticsMetrics()

        assertThat(mobileApp.submitAnalyticsKeys(AnalyticsWindow(startDate, endDate), metrics), equalTo(OK))

        approver.assertAthenaQueryReturnsCorrect(deviceModel, metrics)
    }

    @Test
    fun `submit android analytics data`(approver: Approver) {
        val deviceModel = MobileDeviceModel(UUID.randomUUID().toString())
        val mobileApp = MobileApp(client, config, os = Android, model = deviceModel)
        val metrics = populatedAnalyticsMetrics()

        assertThat(mobileApp.submitAnalyticsKeys(AnalyticsWindow(startDate, endDate), metrics), equalTo(OK))

        approver.assertAthenaQueryReturnsCorrect(deviceModel, metrics)
    }

    private fun Approver.assertAthenaQueryReturnsCorrect(
        deviceModel: MobileDeviceModel,
        analyticsMetrics: AnalyticsMetrics
    ) {
        assertWithin(Duration.ofSeconds(config.analytics_submission_ingestion_interval.toLong() * 4)) {
            val dataFromAthena = analytics.getRecordedAnalyticsFor(deviceModel)

            val fieldsAndValuesWeSent = Http4kJackson.fields(Http4kJackson.asJsonObject(analyticsMetrics))
                .map { it.first.toLowerCase() to it.second.toString() }

            val interestingFieldsFromAthena =
                dataFromAthena.filter { it.first in fieldsAndValuesWeSent.map(Pair<String, String>::first) }

            val interestingFieldsFromAthenaCsv =
                interestingFieldsFromAthena.joinToString("\n") { it.first + "," + it.second }

            assertApproved(interestingFieldsFromAthenaCsv)
        }
    }

    private fun populatedAnalyticsMetrics() = AnalyticsMetrics().apply {
        var counter = 0L
        cumulativeDownloadBytes = counter++
        cumulativeUploadBytes = counter++
        cumulativeCellularDownloadBytes = counter++
        cumulativeCellularUploadBytes = counter++
        cumulativeWifiDownloadBytes = counter++
        cumulativeWifiUploadBytes = counter++
        checkedIn = counter++.toInt()
        canceledCheckIn = counter++.toInt()
        receivedVoidTestResult = counter++.toInt()
        isIsolatingBackgroundTick = counter++.toInt()
        hasHadRiskyContactBackgroundTick = counter++.toInt()
        receivedPositiveTestResult = counter++.toInt()
        receivedNegativeTestResult = counter++.toInt()
        hasSelfDiagnosedPositiveBackgroundTick = counter++.toInt()
        completedQuestionnaireAndStartedIsolation = counter++.toInt()
        encounterDetectionPausedBackgroundTick = counter++.toInt()
        completedQuestionnaireButDidNotStartIsolation = counter++.toInt()
        totalBackgroundTasks = counter++.toInt()
        runningNormallyBackgroundTick = counter++.toInt()
        completedOnboarding = counter++.toInt()
        receivedVoidTestResultEnteredManually = counter++.toInt()
        receivedPositiveTestResultEnteredManually = counter++.toInt()
        receivedNegativeTestResultEnteredManually = counter++.toInt()
        receivedVoidTestResultViaPolling = counter++.toInt()
        receivedPositiveTestResultViaPolling = counter++.toInt()
        receivedNegativeTestResultViaPolling = counter++.toInt()
        hasSelfDiagnosedBackgroundTick = counter++.toInt()
        hasTestedPositiveBackgroundTick = counter++.toInt()
        isIsolatingForSelfDiagnosedBackgroundTick = counter++.toInt()
        isIsolatingForTestedPositiveBackgroundTick = counter++.toInt()
        isIsolatingForHadRiskyContactBackgroundTick = counter++.toInt()
        receivedRiskyContactNotification = counter++.toInt()
        startedIsolation = counter++.toInt()
        receivedPositiveTestResultWhenIsolatingDueToRiskyContact = counter++.toInt()
        receivedActiveIpcToken = counter++.toInt()
        haveActiveIpcTokenBackgroundTick = counter++.toInt()
        selectedIsolationPaymentsButton = counter++.toInt()
        launchedIsolationPaymentsApplication = counter++.toInt()
        receivedPositiveLFDTestResultViaPolling = counter++.toInt()
        receivedNegativeLFDTestResultViaPolling = counter++.toInt()
        receivedVoidLFDTestResultViaPolling = counter++.toInt()
        receivedPositiveLFDTestResultEnteredManually = counter++.toInt()
        receivedNegativeLFDTestResultEnteredManually = counter++.toInt()
        receivedVoidLFDTestResultEnteredManually = counter++.toInt()
        hasTestedLFDPositiveBackgroundTick = counter++.toInt()
        isIsolatingForTestedLFDPositiveBackgroundTick = counter++.toInt()
        totalExposureWindowsNotConsideredRisky = counter++.toInt()
        totalExposureWindowsConsideredRisky = counter++.toInt()
        acknowledgedStartOfIsolationDueToRiskyContact = counter++.toInt()
        hasRiskyContactNotificationsEnabledBackgroundTick = counter++.toInt()
        totalRiskyContactReminderNotifications = counter++.toInt()
        receivedUnconfirmedPositiveTestResult = counter++.toInt()
        isIsolatingForUnconfirmedTestBackgroundTick = counter++.toInt()
        launchedTestOrdering = counter++.toInt()
        didHaveSymptomsBeforeReceivedTestResult = counter++.toInt()
        didRememberOnsetSymptomsDateBeforeReceivedTestResult = counter++.toInt()
        didAskForSymptomsOnPositiveTestEntry = counter++.toInt()
        declaredNegativeResultFromDCT = counter++.toInt()
        receivedPositiveSelfRapidTestResultViaPolling = counter++.toInt()
        receivedNegativeSelfRapidTestResultViaPolling = counter++.toInt()
        receivedVoidSelfRapidTestResultViaPolling = counter++.toInt()
        receivedPositiveSelfRapidTestResultEnteredManually = counter++.toInt()
        receivedNegativeSelfRapidTestResultEnteredManually = counter++.toInt()
        receivedVoidSelfRapidTestResultEnteredManually = counter++.toInt()
        isIsolatingForTestedSelfRapidPositiveBackgroundTick = counter++.toInt()
        hasTestedSelfRapidPositiveBackgroundTick = counter++.toInt()
        receivedRiskyVenueM1Warning = counter++.toInt()
        receivedRiskyVenueM2Warning = counter++.toInt()
        hasReceivedRiskyVenueM2WarningBackgroundTick = counter++.toInt()
        totalAlarmManagerBackgroundTasks = counter++.toInt()
        missingPacketsLast7Days = counter++.toInt()
        consentedToShareVenueHistory = counter++.toInt()
        askedToShareVenueHistory = counter++.toInt()
        askedToShareExposureKeysInTheInitialFlow = counter++.toInt()
        consentedToShareExposureKeysInTheInitialFlow = counter++.toInt()
        totalShareExposureKeysReminderNotifications = counter++.toInt()
        consentedToShareExposureKeysInReminderScreen = counter++.toInt()
        successfullySharedExposureKeys = counter++.toInt()
        didSendLocalInfoNotification = counter++.toInt()
        didAccessLocalInfoScreenViaNotification = counter++.toInt()
        didAccessLocalInfoScreenViaBanner = counter++.toInt()
        isDisplayingLocalInfoBackgroundTick = counter.toInt()
    }
}
