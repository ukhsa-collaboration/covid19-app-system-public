package uk.nhs.nhsx.analyticssubmission

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetadata
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.analyticssubmission.policy.PolicyConfig
import uk.nhs.nhsx.analyticssubmission.policy.TTSPDiscontinuationPolicy
import uk.nhs.nhsx.analyticssubmission.policy.TTSPDiscontinuationPolicy.Companion.default
import uk.nhs.nhsx.core.events.RecordingEvents
import java.time.Instant
import java.time.OffsetDateTime

class MetricsScrubberTest {

    private val events = RecordingEvents()

    val payload = ClientAnalyticsSubmissionPayload(
        analyticsWindow = AnalyticsWindow(
            startDate = Instant.parse("2022-04-06T22:00:00Z"),
            endDate = Instant.parse("2022-04-06T23:59:00Z")
        ),
        metadata = AnalyticsMetadata(
            postalDistrict = "AL2",
            deviceModel = "",
            operatingSystemVersion = "",
            latestApplicationVersion = "",
            localAuthority = "E07000098"
        ),
        metrics = AnalyticsMetrics().apply {
            receivedActiveIpcToken = 1
            haveActiveIpcTokenBackgroundTick = 2
            selectedIsolationPaymentsButton = 3
            launchedIsolationPaymentsApplication = 4
        },
        includesMultipleApplicationVersions = false
    )

    @ParameterizedTest
    @ValueSource(strings = ["2022-04-07T00:00+01:00", "2022-04-07T00:01+01:00", "2022-04-07T08:00+01:00", "2022-04-14T00:01+01:00"])
    fun `scrubs TTSP fields after the 7th of April 2022`(now: String) {
        val scrubber = MetricsScrubber(
            events = events,
            clock = { OffsetDateTime.parse(now).toInstant() },
            config = PolicyConfig(listOf(TTSPDiscontinuationPolicy(default)))
        )

        val scrubbed = scrubber.scrub(payload)

        expectThat(scrubbed)
            .get { metrics }
            .and {
                get { receivedActiveIpcToken }.isNull()
                get { haveActiveIpcTokenBackgroundTick }.isNull()
                get { selectedIsolationPaymentsButton }.isNull()
                get { launchedIsolationPaymentsApplication }.isNull()
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["2022-04-06T23:59+01:00", "2022-04-01T00:00+01:00"])
    fun `does not scrub TTSP fields before the 7th of April 2022`(now: String) {
        val scrubber = MetricsScrubber(
            events = events,
            clock = { OffsetDateTime.parse(now).toInstant() },
            config = PolicyConfig(listOf(TTSPDiscontinuationPolicy(default)))
        )

        val scrubbed = scrubber.scrub(payload)

        expectThat(scrubbed)
            .get { metrics }
            .and {
                get { receivedActiveIpcToken }.isNotNull()
                get { haveActiveIpcTokenBackgroundTick }.isNotNull()
                get { selectedIsolationPaymentsButton }.isNotNull()
                get { launchedIsolationPaymentsApplication }.isNotNull()
            }
    }
}
