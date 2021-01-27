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
import smoke.actors.MobileOS
import smoke.actors.MobileOS.Android
import smoke.env.SmokeTests
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.testhelper.http4k.assertApproved
import uk.nhs.nhsx.testhelper.junit.assertWithin
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import org.http4k.format.Jackson as Http4kJackson

@ExtendWith(ApprovalTest::class)
class AnalyticsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val startDate = OffsetDateTime.now().minusDays(1).format(DateFormatValidator.formatter).toString()
    private val endDate = OffsetDateTime.now().plusDays(1).format(DateFormatValidator.formatter).toString()
    private val analytics = Analytics(config)

    @Test
    fun `submit ios analytics data`(approver: Approver) {
        val deviceModel = MobileDeviceModel(UUID.randomUUID().toString())
        val mobileApp = MobileApp(client, config, MobileOS.iOS, deviceModel)
        val metrics = populatedAnalyticsMetrics()

        assertThat(mobileApp.submitAnalyticsKeys(AnalyticsWindow(startDate, endDate), metrics), equalTo(OK))

        approver.assertAthenaQueryReturnsCorrect(deviceModel, metrics)
    }

    @Test
    fun `submit android analytics data`(approver: Approver) {
        val deviceModel = MobileDeviceModel(UUID.randomUUID().toString())
        val mobileApp = MobileApp(client, config, Android, deviceModel)
        val metrics = populatedAnalyticsMetrics()

        assertThat(mobileApp.submitAnalyticsKeys(AnalyticsWindow(startDate, endDate), metrics), equalTo(OK))

        approver.assertAthenaQueryReturnsCorrect(deviceModel, metrics)
    }

    private fun Approver.assertAthenaQueryReturnsCorrect(deviceModel: MobileDeviceModel, analyticsMetrics: AnalyticsMetrics) {
        assertWithin(Duration.ofSeconds(config.analyticsSubmissionIngestionInterval.toLong() * 2)) {
            val dataFromAthena = analytics.getRecordedAnalyticsFor(deviceModel)

            val fieldsAndValuesWeSent = Http4kJackson.fields(Http4kJackson.asJsonObject(analyticsMetrics)).map { it.first.toLowerCase() to it.second.toString() }

            val interestingFieldsFromAthena = dataFromAthena.filter { it.first in fieldsAndValuesWeSent.map { it.first } }

            val interestingFieldsFromAthenaCsv = interestingFieldsFromAthena.joinToString("\n") { it.first + "," + it.second }

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
        totalExposureWindowsConsideredRisky = counter.toInt()
        
    }
}
