package uk.nhs.nhsx.highriskvenuesupload

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasEntry
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.highriskvenuesupload.VenuesUploadResult.ValidationError
import uk.nhs.nhsx.testhelper.TestDatedSigner
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.asString
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.metadata
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.userMetadata
import uk.nhs.nhsx.testhelper.data.TestData.RISKY_VENUES_UPLOAD_PAYLOAD
import uk.nhs.nhsx.testhelper.data.TestData.STORED_RISKY_VENUES_UPLOAD_PAYLOAD
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.getObject
import uk.nhs.nhsx.testhelper.mocks.isEmpty
import java.util.*

class HighRiskVenuesUploadServiceTest {

    private val s3BucketName = BucketName.of(UUID.randomUUID().toString())
    private val s3ObjKeyName = ObjectKey.of("my-object-key")
    private val cloudFrontDistId = UUID.randomUUID().toString()
    private val cloudFrontInvPattern = UUID.randomUUID().toString()

    private val s3 = FakeS3()
    private val awsCloudFront = mockk<AwsCloudFront> {
        every { invalidateCache(any(), any()) } just runs
    }

    private val parser = HighRiskVenueCsvParser()
    private val config = HighRiskVenuesUploadConfig.of(
        bucket = s3BucketName,
        key = s3ObjKeyName,
        cloudFrontDistId = cloudFrontDistId,
        cloudFrontInvalidationPattern = cloudFrontInvPattern
    )
    private val testSigner = TestDatedSigner("date")
    private val service = HighRiskVenuesUploadService(
        config = config,
        signer = testSigner,
        s3Client = s3,
        awsCloudFront = awsCloudFront,
        parser = parser
    )

    @Test
    fun `uploads csv`() {
        service.upload(RISKY_VENUES_UPLOAD_PAYLOAD)
        verifyHappyPath(STORED_RISKY_VENUES_UPLOAD_PAYLOAD)
    }

    @Test
    fun `uploads empty csv`() {
        service.upload("# venue_id, start_time, end_time, message_type, optional_parameter")
        verifyHappyPath("""{"venues":[]}""")
    }

    @Test
    fun `uploads when s 3 object does not exist`() {
        service.upload(RISKY_VENUES_UPLOAD_PAYLOAD)
        verifyHappyPath(STORED_RISKY_VENUES_UPLOAD_PAYLOAD)
    }

    @Test
    fun `validation error if empty body`() {
        val result = service.upload("")

        expectThat(s3).isEmpty()
        expectThat(result)
            .isA<ValidationError>()
            .get(ValidationError::message)
            .isEqualTo("validation error: No payload")
    }

    @Test
    fun `validation error if whitespace body`() {
        val result = service.upload("    ")

        expectThat(s3).isEmpty()
        expectThat(result)
            .isA<ValidationError>()
            .get(ValidationError::message)
            .isEqualTo("validation error: No payload")
    }

    @Test
    fun `validation error if invalid header`() {
        val result = service.upload("# start_time, venue_id, end_time")

        expectThat(s3).isEmpty()
        expectThat(result)
            .isA<ValidationError>()
            .get(ValidationError::message)
            .isEqualTo("validation error: Invalid header. Expected [venue_id, start_time, end_time, message_type, optional_parameter]")
    }

    private fun verifyHappyPath(payload: String) {
        expectThat(s3)
            .getBucket(s3BucketName)
            .getObject(s3ObjKeyName)
            .and { content.asString().isEqualTo(payload) }
            .and {
                metadata.userMetadata
                    .hasEntry("Signature", """keyId="some-key",signature="AAECAwQ="""")
                    .hasEntry("Signature-Date", "date")
            }

        verify(exactly = 1) {
            awsCloudFront.invalidateCache(cloudFrontDistId, cloudFrontInvPattern)
        }
    }
}
