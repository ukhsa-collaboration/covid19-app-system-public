package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionHandlerTest.Companion.analyticsPayloadFrom
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Json.readJsonOrThrow
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.events
import java.time.Instant

class AnalyticsSubmissionServiceTest {

    private val objectKeyNameProvider = ObjectKeyNameProvider { ObjectKey.of("foo") }
    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()
    private val events = RecordingEvents()
    private val clock = { Instant.parse("2020-02-01T00:00:00Z") }

    @Test
    fun `uploads to firehose when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            firehoseIngestEnabled = true
        )

        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service = AnalyticsSubmissionService(config, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }

        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `uploads to firehose with optional local authority when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            firehoseIngestEnabled = true
        )

        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service = AnalyticsSubmissionService(config, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }

        expectThat(events).containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `filters message if start date is in the future`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            firehoseIngestEnabled = true
        )
        val clock = { Instant.parse("2020-02-01T00:00:00Z") }
        val service =
            AnalyticsSubmissionService(config, objectKeyNameProvider, kinesisFirehose, events, clock)

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
        val config = AnalyticsConfig(
            "firehoseStreamName",
            firehoseIngestEnabled = true
        )
        val clock = { Instant.parse("2020-02-01T00:00:00Z") }
        val service =
            AnalyticsSubmissionService(config, objectKeyNameProvider, kinesisFirehose, events, clock)

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

    private fun clientPayload(startDate: String = "2020-01-27T23:00:00Z", endDate: String = "2020-01-28T22:59:00Z") =
        readJsonOrThrow<ClientAnalyticsSubmissionPayload>(
            analyticsPayloadFrom(
                startDate = startDate,
                endDate = endDate,
                postDistrict = "AB13",
                localAuthority = "E06000051"
            )
        )
}
