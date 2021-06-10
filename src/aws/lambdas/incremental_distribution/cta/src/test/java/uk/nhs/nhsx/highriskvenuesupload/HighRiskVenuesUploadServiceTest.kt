package uk.nhs.nhsx.highriskvenuesupload

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.signature.SigningHeadersTest.Companion.matchesMeta
import uk.nhs.nhsx.highriskvenuesupload.VenuesUploadResult.ValidationError
import uk.nhs.nhsx.testhelper.TestDatedSigner
import uk.nhs.nhsx.testhelper.data.TestData.RISKY_VENUES_UPLOAD_PAYLOAD
import uk.nhs.nhsx.testhelper.data.TestData.STORED_RISKY_VENUES_UPLOAD_PAYLOAD
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import java.nio.charset.StandardCharsets

class HighRiskVenuesUploadServiceTest {

    private val s3BucketName = BucketName.of("some-bucket")
    private val s3ObjKeyName = ObjectKey.of("some-key")
    private val cloudFrontDistId = "some-dist-id"
    private val cloudFrontInvPattern = "some-pattern"

    private val s3 = FakeS3Storage()
    private val awsCloudFront = mockk<AwsCloudFront>().also {
        every { it.invalidateCache(any(), any()) } returns Unit
    }

    private val parser = HighRiskVenueCsvParser()
    private val config = HighRiskVenuesUploadConfig.of(
        s3BucketName,
        s3ObjKeyName,
        cloudFrontDistId,
        cloudFrontInvPattern
    )
    private val testSigner = TestDatedSigner("date")
    private val service = HighRiskVenuesUploadService(
        config,
        testSigner,
        s3,
        awsCloudFront,
        parser
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

        assertThat(s3.count).isEqualTo(0)
        assertThat(result).isInstanceOfSatisfying(ValidationError::class.java) {
            assertThat(it.message).isEqualTo("validation error: No payload")
        }
    }

    @Test
    fun `validation error if whitespace body`() {
        val result = service.upload("    ")

        assertThat(s3.count).isEqualTo(0)
        assertThat(result).isInstanceOfSatisfying(ValidationError::class.java) {
            assertThat(it.message).isEqualTo("validation error: No payload")
        }
    }

    @Test
    fun `validation error if invalid header`() {
        val result = service.upload("# start_time, venue_id, end_time")

        assertThat(s3.count).isEqualTo(0)
        assertThat(result).isInstanceOfSatisfying(ValidationError::class.java) {
            assertThat(it.message).isEqualTo("validation error: Invalid header. Expected [venue_id, start_time, end_time, message_type, optional_parameter]")
        }
    }

    private fun verifyHappyPath(payload: String) {
        assertThat(s3.count, equalTo(1))
        assertThat(s3.bucket, equalTo(s3BucketName))
        assertThat(s3.name, equalTo(s3ObjKeyName))
        assertThat(s3.bytes.toArray(), equalTo(payload.toByteArray(StandardCharsets.UTF_8)))
        assertThat(s3.meta, matchesMeta(testSigner.keyId, "AAECAwQ=", "date"))
        verify(exactly = 1) { awsCloudFront.invalidateCache(cloudFrontDistId, cloudFrontInvPattern) }
    }
}
