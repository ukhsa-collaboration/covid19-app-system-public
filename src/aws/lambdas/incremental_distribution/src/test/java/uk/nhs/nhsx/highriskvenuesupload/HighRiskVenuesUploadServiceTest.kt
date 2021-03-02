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
import uk.nhs.nhsx.testhelper.TestDatedSigner
import uk.nhs.nhsx.testhelper.data.TestData.RISKY_VENUES_UPLOAD_PAYLOAD
import uk.nhs.nhsx.testhelper.data.TestData.STORED_RISKY_VENUES_UPLOAD_PAYLOAD
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import java.nio.charset.StandardCharsets

class HighRiskVenuesUploadServiceTest {

    private val s3 = FakeS3Storage()
    private val awsCloudFront = mockk<AwsCloudFront>().also {
        every { it.invalidateCache(any(), any()) } returns Unit
    }

    private val parser = HighRiskVenueCsvParser()
    private val config = HighRiskVenuesUploadConfig(
        s3BucketName, s3ObjKeyName,
        cloudFrontDistId, cloudFrontInvPattern
    )
    private val testSigner = TestDatedSigner("date")
    private val service = HighRiskVenuesUploadService(
        config, testSigner, s3, awsCloudFront, parser
    )

    @Test
    fun uploadsCsv() {
        service.upload(RISKY_VENUES_UPLOAD_PAYLOAD)
        verifyHappyPath(STORED_RISKY_VENUES_UPLOAD_PAYLOAD)
    }

    @Test
    fun uploadsEmptyCsv() {
        service.upload("# venue_id, start_time, end_time")
        verifyHappyPath("{\"venues\":[]}")
    }

    @Test
    fun uploadsWhenS3ObjectDoesNotExist() {
        service.upload(RISKY_VENUES_UPLOAD_PAYLOAD)
        verifyHappyPath(STORED_RISKY_VENUES_UPLOAD_PAYLOAD)
    }

    @Test
    fun validationErrorIfNoBody() {
        val result = service.upload(null)
        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError)
        assertThat(result.message).isEqualTo("validation error: No payload")
        assertThat(s3.count).isEqualTo(0)
    }

    @Test
    fun validationErrorIfEmptyBody() {
        val result = service.upload("")
        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError)
        assertThat(result.message).isEqualTo("validation error: No payload")
        assertThat(s3.count).isEqualTo(0)
    }

    @Test
    fun validationErrorIfWhitespaceBody() {
        val result = service.upload("    ")
        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError)
        assertThat(result.message).isEqualTo("validation error: No payload")
        assertThat(s3.count).isEqualTo(0)
    }

    @Test
    fun validationErrorIfInvalidHeader() {
        val result = service.upload("# start_time, venue_id, end_time")
        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError)
        assertThat(result.message).isEqualTo("validation error: Invalid header")
        assertThat(s3.count).isEqualTo(0)
    }

    private fun verifyHappyPath(payload: String) {
        assertThat(s3.count, equalTo(1))
        assertThat(s3.bucket, equalTo(s3BucketName))
        assertThat(s3.name, equalTo(s3ObjKeyName))
        assertThat(s3.bytes.toArray(), equalTo(payload.toByteArray(StandardCharsets.UTF_8)))
        assertThat(s3.meta, matchesMeta(testSigner.keyId, "AAECAwQ=", "date"))
        verify(exactly = 1) { awsCloudFront.invalidateCache(cloudFrontDistId, cloudFrontInvPattern) }
    }

    companion object {
        private val s3BucketName = BucketName.of("some-bucket")
        private val s3ObjKeyName = ObjectKey.of("some-key")
        private const val cloudFrontDistId = "some-dist-id"
        private const val cloudFrontInvPattern = "some-pattern"
    }
}
