package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import uk.nhs.nhsx.core.ContentType
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.io.ByteArrayInputStream
import java.net.URL
import java.time.Instant
import java.util.*

class FakeInteropDiagnosisKeysS3(
    private val objectSummaries: List<S3ObjectSummary>
) : AwsS3 {

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        metaHeaders: List<MetaHeader>
    ) = Unit

    override fun getObjectSummaries(bucketName: BucketName) = objectSummaries

    override fun getObject(locator: Locator): Optional<S3Object> {
        val mostRecentKeyRollingStart =
            ENIntervalNumber.enIntervalNumberFromTimestamp(Instant.now()).enIntervalNumber / 144 * 144

        val json = Json.toJson(
            StoredTemporaryExposureKeyPayload(
                listOf(makeKey(mostRecentKeyRollingStart - 144))
            )
        )

        return Optional.of(S3Object().apply {
            setObjectContent(ByteArrayInputStream(json.toByteArray()))
        })
    }

    override fun deleteObject(locator: Locator) = Unit
    override fun copyObject(from: Locator, to: Locator) = Unit
    override fun getSignedURL(locator: Locator, expiration: Date) =
        Optional.of(URL("https://example.com"))

    private fun makeKey(keyStartTime: Long) =
        StoredTemporaryExposureKey(
            getEncodedKeyData(),
            Math.toIntExact(keyStartTime),
            144,
            7
        )

    fun getEncodedKeyData() = "3/TzKOK2u0O/eHeK4R0VSg=="
}
