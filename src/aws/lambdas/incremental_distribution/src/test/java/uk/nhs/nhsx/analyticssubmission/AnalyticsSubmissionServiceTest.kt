package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionHandlerTest.Companion.iOSPayloadFrom
import uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionHandlerTest.Companion.iOSPayloadFromWithLocalAuthority
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Jackson.readMaybe
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.RecordingEvents

class AnalyticsSubmissionServiceTest {

    private val s3Storage = mockk<S3Storage>()
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()
    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()
    private val events = RecordingEvents()

    @Test
    fun `uploads to s3 when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            true,
            false,
            BucketName.of("some-bucket-name")
        )

        every { objectKeyNameProvider.generateObjectKeyName() } returns mockk(relaxed = true)
        every { s3Storage.upload(any(), any(), any()) } just Runs

        val service = AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events)

        service.accept(clientPayload())

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
    }

    @Test
    fun `uploads to s3 with optional local authority when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            true,
            false,
            BucketName.of("some-bucket-name")
        )

        every { objectKeyNameProvider.generateObjectKeyName() } returns mockk(relaxed = true)
        every { s3Storage.upload(any(), any(), any()) } just Runs

        val service = AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events)

        service.accept(clientPayloadWithLocalAuthority())

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
    }


    @Test
    fun `uploads to firehose when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            false,
            true,
            BucketName.of("some-bucket-name")
        )

        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service = AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events)

        service.accept(clientPayload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }
    }

    @Test
    fun `uploads to firehose with optional local authority  when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            false,
            true,
            BucketName.of("some-bucket-name")
        )

        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service = AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose, events)

        service.accept(clientPayloadWithLocalAuthority())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }
    }

    private fun clientPayload() =
        deserialize(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"))

    private fun clientPayloadWithLocalAuthority() =
        deserialize(iOSPayloadFromWithLocalAuthority("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))

    private fun deserialize(json: String) =
        readMaybe(
            json,
            ClientAnalyticsSubmissionPayload::class.java,
            {}).orElseThrow()
}
