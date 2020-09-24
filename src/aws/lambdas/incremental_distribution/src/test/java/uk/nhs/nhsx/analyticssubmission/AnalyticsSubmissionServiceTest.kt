package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import io.mockk.*
import org.junit.Test
import uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionHandlerTest.iOSPayloadFrom
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Jackson.deserializeMaybe
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage

class AnalyticsSubmissionServiceTest {

    private val s3Storage = mockk<S3Storage>()
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()
    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()

    @Test
    fun `uploads to s3 when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            true,
            false,
            "some-bucket-name"
        )

        every { objectKeyNameProvider.generateObjectKeyName() } returns mockk(relaxed = true)
        every { s3Storage.upload(any(), any(), any()) } just Runs

        val service = AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose)

        service.accept(clientPayload())

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
    }

    @Test
    fun `uploads to firehose when feature is enabled`() {
        val config = AnalyticsConfig(
            "firehoseStreamName",
            false,
            true,
            "some-bucket-name"
        )

        every { kinesisFirehose.putRecord(any()) } returns mockk()

        val service = AnalyticsSubmissionService(config, s3Storage, objectKeyNameProvider, kinesisFirehose)

        service.accept(clientPayload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }
    }

    private fun clientPayload() =
        deserialize(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"))

    private fun deserialize(json: String) =
        deserializeMaybe(json, ClientAnalyticsSubmissionPayload::class.java).orElseThrow()
}