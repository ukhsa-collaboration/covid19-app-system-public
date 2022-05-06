package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsKey
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetadata
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.analyticssubmission.policy.PolicyConfig
import uk.nhs.nhsx.analyticssubmission.policy.TTSPDiscontinuationPolicy
import uk.nhs.nhsx.analyticssubmission.policy.TTSPDiscontinuationPolicy.Companion.default
import uk.nhs.nhsx.core.AppServicesJson.mapper
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Json.readJsonOrThrow
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.events
import java.nio.charset.Charset
import java.time.Instant
import java.time.OffsetDateTime
import uk.nhs.nhsx.analyticssubmission.AnalyticsMapNullRemover.removeNullValues

class AnalyticsSubmissionServiceTest {

    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()
    private val events = RecordingEvents()
    private val clock = { Instant.parse("2020-02-01T00:00:00Z") }
    private val eventStartDate = Instant.parse("2020-01-27T23:00:00Z")
    private val eventEndDate = Instant.parse("2020-01-28T22:59:00Z")

    @Test
    fun `uploads to firehose when feature is enabled`() {
        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service = AnalyticsSubmissionService(firehoseConfig(enabled = true), kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }
        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `does not upload to firehose when feature is disabled`() {
        val service = AnalyticsSubmissionService(firehoseConfig(enabled = false), kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 0) { kinesisFirehose.putRecord(any()) }
        expectThat(events).containsExactly(AnalyticsSubmissionDisabled::class)
    }

    @Test
    fun `uploads to firehose with optional local authority`() {
        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service = AnalyticsSubmissionService(firehoseConfig(enabled = true), kinesisFirehose, events, clock)

        service.accept(clientPayload(localAuthority = null))

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }
        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `filters message if start date is in the future`() {
        val clock = { Instant.parse("2020-02-01T00:00:00Z") }
        val service = AnalyticsSubmissionService(firehoseConfig(enabled = true), kinesisFirehose, events, clock)

        service.accept(
            clientPayload(
                startDate = "2021-02-02T00:00:01Z",
                endDate = "2021-02-02T00:00:00Z"
            )
        )

        verify(exactly = 0) { kinesisFirehose.putRecord(any()) }
        expectThat(events)
            .containsExactly(AnalyticsSubmissionRejected::class)
            .and {
                events.contains(
                    AnalyticsSubmissionRejected(
                        startDate = "2021-02-02T00:00:01Z",
                        endDate = "2021-02-02T00:00:00Z",
                        deviceModel = "iPhone11,2",
                        appVersion = "3.0",
                        osVersion = "iPhone OS 13.5.1 (17F80)"
                    )
                )
            }
    }

    @Test
    fun `filters message if end date is in the future`() {
        val clock = { Instant.parse("2020-02-01T00:00:00Z") }
        val service = AnalyticsSubmissionService(firehoseConfig(enabled = true), kinesisFirehose, events, clock)

        service.accept(
            clientPayload(
                startDate = "2021-01-01T00:00:00Z",
                endDate = "2021-02-02T00:00:01Z"
            )
        )

        verify(exactly = 0) { kinesisFirehose.putRecord(any()) }
        expectThat(events)
            .containsExactly(AnalyticsSubmissionRejected::class)
            .and {
                events.contains(
                    AnalyticsSubmissionRejected(
                        startDate = "2021-01-01T00:00:00Z",
                        endDate = "2021-02-02T00:00:01Z",
                        deviceModel = "iPhone11,2",
                        appVersion = "3.0",
                        osVersion = "iPhone OS 13.5.1 (17F80)"
                    )
                )
            }
    }

    @Test
    fun `flattens all fields from client payload`() {
        val json = analyticsSubmissionIosComplete(
            startDate = eventStartDate.toString(),
            endDate = eventEndDate.toString(),
            postalDistrict = "AB10",
            useCounter = true
        )
        val clientPayload = readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)

        var counter = 1L

        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "AB10",
            deviceModel = "iPhone11,2",
            operatingSystemVersion = "iPhone OS 13.5.1 (17F80)",
            latestApplicationVersion = "3.0",
            localAuthority = null,
            cumulativeDownloadBytes = counter++.toInt(),
            cumulativeUploadBytes = counter++.toInt(),
            cumulativeCellularDownloadBytes = counter++.toInt(),
            cumulativeCellularUploadBytes = counter++.toInt(),
            cumulativeWifiDownloadBytes = counter++.toInt(),
            cumulativeWifiUploadBytes = counter++.toInt(),
            receivedVoidTestResult = counter++.toInt(),
            isIsolatingBackgroundTick = counter++.toInt(),
            hasHadRiskyContactBackgroundTick = counter++.toInt(),
            receivedPositiveTestResult = counter++.toInt(),
            receivedNegativeTestResult = counter++.toInt(),
            completedQuestionnaireAndStartedIsolation = counter++.toInt(),
            encounterDetectionPausedBackgroundTick = counter++.toInt(),
            completedQuestionnaireButDidNotStartIsolation = counter++.toInt(),
            totalBackgroundTasks = counter++.toInt(),
            runningNormallyBackgroundTick = counter++.toInt(),
            completedOnboarding = counter++.toInt(),
            includesMultipleApplicationVersions = false,
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
            optedOutForContactIsolation = counter++.toInt(),
            optedOutForContactIsolationBackgroundTick = counter++.toInt(),
            appIsUsableBackgroundTick = counter++.toInt(),
            appIsContactTraceableBackgroundTick = counter++.toInt(),
            appIsUsableBluetoothOffBackgroundTick = counter.toInt()
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)

        expectThat(exportedMap).isEqualTo(flattenedNonNull)
        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `merged postal district with matching local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
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

        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)
        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "AL2_AL4_WD7",
            localAuthority = "E07000098",
            includesMultipleApplicationVersions = false
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)

        expectThat(exportedMap).isEqualTo(flattenedNonNull)
        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `merged postal district with null local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
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

        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)
        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "AB13_AB14",
            localAuthority = null,
            includesMultipleApplicationVersions = false
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)

        expectThat(exportedMap).isEqualTo(flattenedNonNull)
        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `merged postal district with non-matching local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
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

        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)
        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "YO60_YO62",
            localAuthority = null,
            includesMultipleApplicationVersions = false
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)

        expectThat(exportedMap).isEqualTo(flattenedNonNull)
        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `merged postal district with invalid local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
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
        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)
        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "YO60_YO62",
            localAuthority = null,
            includesMultipleApplicationVersions = false
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)

        expectThat(exportedMap).isEqualTo(flattenedNonNull)
        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `empty postal district and empty local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
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
        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)
        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "NOT SET",
            localAuthority = null,
            includesMultipleApplicationVersions = false
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)

        expectThat(exportedMap).isEqualTo(flattenedNonNull)
    }

    @Test
    fun `unknown postcode with non-null local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
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
        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)
        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "UNKNOWN",
            localAuthority = "UNKNOWN",
            includesMultipleApplicationVersions = false
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)
        expectThat(exportedMap).isEqualTo(flattenedNonNull)
    }

    @Test
    fun `unknown postcode with null local authority`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
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

        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload)

        val analyticsPayload = analyticsStoredPayload(
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            postalDistrict = "UNKNOWN",
            localAuthority = "UNKNOWN",
            includesMultipleApplicationVersions = false
        )
        val flattenedNonNull = removeNullValues(analyticsPayload)
        expectThat(exportedMap).isEqualTo(flattenedNonNull)
    }

    @Test
    fun `scrubs TTSP data after the policy becomes effective`() {
        val clientPayload = ClientAnalyticsSubmissionPayload(
            analyticsWindow = AnalyticsWindow(eventStartDate, eventEndDate),
            metadata = AnalyticsMetadata(
                postalDistrict = "F4KEP0STC0DE",
                deviceModel = "",
                operatingSystemVersion = "",
                latestApplicationVersion = "",
                localAuthority = null
            ),
            metrics = AnalyticsMetrics().apply {
                receivedActiveIpcToken = 1
                haveActiveIpcTokenBackgroundTick = 2
                selectedIsolationPaymentsButton = 3
                launchedIsolationPaymentsApplication = 4
            },
            includesMultipleApplicationVersions = false
        )

        val now = { OffsetDateTime.parse("2022-04-07T00:00+01:00").toInstant() }

        val exportedMap = invokeAndCaptureFirehosePayload(clientPayload, now)

        expectThat(exportedMap).and {
            not().containsKey("receivedActiveIpcToken")
            not().containsKey("haveActiveIpcTokenBackgroundTick")
            not().containsKey("selectedIsolationPaymentsButton")
            not().containsKey("launchedIsolationPaymentsApplication")
        }
    }



    private fun invokeAndCaptureFirehosePayload(
        clientPayload: ClientAnalyticsSubmissionPayload,
        now: Clock = clock
    ): Map<String, Any?> {
        val slot = slot<PutRecordRequest>()
        every { kinesisFirehose.putRecord(capture(slot)) } answers { PutRecordResult() }

        AnalyticsSubmissionService(
            config = firehoseConfig(enabled = true),
            kinesisFirehose = kinesisFirehose,
            events = events,
            clock = now
        ).accept(clientPayload)

        val exportedJson = String(slot.captured.record.data.array(), Charset.forName("UTF-8"))
        return mapper.readValue(exportedJson)
    }

    private fun firehoseConfig(enabled: Boolean) = AnalyticsConfig(
        firehoseStreamName = "firehoseStreamName",
        firehoseIngestEnabled = enabled,
        policyConfig = PolicyConfig(listOf(TTSPDiscontinuationPolicy(default)))
    )

    private fun clientPayload(
        startDate: String = eventStartDate.toString(),
        endDate: String = eventEndDate.toString(),
        localAuthority: String? = "E06000051"
    ) = readJsonOrThrow<ClientAnalyticsSubmissionPayload>(
        analyticsSubmissionIos(
            startDate = startDate,
            endDate = endDate,
            localAuthority = localAuthority
        )
    )
}
