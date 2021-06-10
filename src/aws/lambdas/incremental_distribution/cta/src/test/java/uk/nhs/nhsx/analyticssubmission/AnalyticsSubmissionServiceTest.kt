package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionHandlerTest.Companion.iOSPayloadFrom
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Json.readJsonOrThrow
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.RecordingEvents
import java.time.Instant

class AnalyticsSubmissionServiceTest {

    private val s3Storage = mockk<S3Storage>()
    private val objectKeyNameProvider = ObjectKeyNameProvider { ObjectKey.of("foo") }
    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()
    private val events = RecordingEvents()
    private val clock = { Instant.parse("2020-02-01T00:00:00Z") }

    @Test
    fun `uploads to s3 when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            s3IngestEnabled = true,
            firehoseIngestEnabled = false,
            bucketName = BucketName.of("some-bucket-name")
        )

        every { s3Storage.upload(any(), any(), any()) } just Runs

        val service =
            AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }

        events.containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `uploads to s3 with optional local authority when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            s3IngestEnabled = true,
            firehoseIngestEnabled = false,
            bucketName = BucketName.of("some-bucket-name")
        )

        every { s3Storage.upload(any(), any(), any()) } just Runs

        val service =
            AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }

        events.containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `uploads to firehose when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            s3IngestEnabled = false,
            firehoseIngestEnabled = true,
            bucketName = BucketName.of("some-bucket-name")
        )

        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service =
            AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }

        events.containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `uploads to firehose with optional local authority when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            s3IngestEnabled = false,
            firehoseIngestEnabled = true,
            bucketName = BucketName.of("some-bucket-name")
        )

        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service =
            AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(clientPayload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }

        events.containsExactly(AnalyticsSubmissionUploaded::class)
    }

    @Test
    fun `filters message if start date is in the future`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            s3IngestEnabled = true,
            firehoseIngestEnabled = true,
            bucketName = BucketName.of("some-bucket-name")
        )
        val clock = { Instant.parse("2020-02-01T00:00:00Z") }
        val service =
            AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(
            clientPayload(
                startDate = "2021-02-02T00:00:01Z",
                endDate = "2021-02-02T00:00:00Z"
            )
        )

        verify(exactly = 0) { kinesisFirehose.putRecord(any()) }
        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }

        events.containsExactly(AnalyticsSubmissionRejected::class)
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

    @Test
    fun `filters message if end date is in the future`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            s3IngestEnabled = true,
            firehoseIngestEnabled = true,
            bucketName = BucketName.of("some-bucket-name")
        )
        val clock = { Instant.parse("2020-02-01T00:00:00Z") }
        val service =
            AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events, clock)

        service.accept(
            clientPayload(
                startDate = "2021-01-01T00:00:00Z",
                endDate = "2021-02-02T00:00:01Z"
            )
        )

        verify(exactly = 0) { kinesisFirehose.putRecord(any()) }
        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }

        events.containsExactly(AnalyticsSubmissionRejected::class)
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

    private fun clientPayload(startDate: String = "2020-01-27T23:00:00Z", endDate: String = "2020-01-28T22:59:00Z") =
        readJsonOrThrow<ClientAnalyticsSubmissionPayload>(iOSPayloadFrom(startDate, endDate, "AB13"))
}
