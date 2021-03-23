package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import uk.nhs.nhsx.core.ContentType
import uk.nhs.nhsx.core.Jackson
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
import java.util.Date
import java.util.Optional

class FakeDiagnosisKeysS3(
    private val objectSummaries: List<S3ObjectSummary>,
    private val objectsKeysToSkip: List<String> = listOf()
) : AwsS3 {

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        metaHeaders: List<MetaHeader>
    ) {
        // noop
    }

    override fun getObjectSummaries(bucketName: BucketName): List<S3ObjectSummary> = objectSummaries

    override fun getObject(locator: Locator): Optional<S3Object> {
        if (objectsKeysToSkip.contains(locator.key.value)) {
            return Optional.empty()
        }

        val mostRecentKeyRollingStart =
            ENIntervalNumber.enIntervalNumberFromTimestamp(Instant.now()).enIntervalNumber / 144 * 144

        val json = Jackson.toJson(
            StoredTemporaryExposureKeyPayload(
                listOf(makeKey(locator, mostRecentKeyRollingStart - 144))
            )
        )

        return Optional.of(S3Object().apply {
            setObjectContent(ByteArrayInputStream(json.toByteArray()))
        })
    }

    override fun deleteObject(locator: Locator) {
        // noop
    }

    override fun getSignedURL(locator: Locator, expiration: Date) =
        Optional.of(URL("https://example.com"))

    private fun makeKey(locator: Locator, keyStartTime: Long): StoredTemporaryExposureKey =
        StoredTemporaryExposureKey(locator.key.value, Math.toIntExact(keyStartTime), 144, 7)
}
