package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.google.common.io.ByteSource
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class FakeDiagnosisKeysS3(private val objectSummaries: List<S3ObjectSummary>,
                          private val objectsKeysToSkip: List<String> = listOf()) : AwsS3 {

    override fun upload(locator: Locator?, contentType: ContentType?, bytes: ByteSource?, vararg meta: MetaHeader?) {
        // noop
    }

    override fun getObjectSummaries(bucketName: BucketName): List<S3ObjectSummary> = objectSummaries

    override fun getObject(locator: Locator): Optional<S3Object> {
        if (objectsKeysToSkip.contains(locator.key.value)) {
            return Optional.empty()
        }
        val mostRecentKeyRollingStart = ENIntervalNumber.enIntervalNumberFromTimestamp(
            Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
        ).enIntervalNumber / 144 * 144
        val json = Jackson.toJson(StoredTemporaryExposureKeyPayload(
            listOf(makeKey(locator, mostRecentKeyRollingStart - 144))
        ))
        val s3Object = S3Object()
        s3Object.setObjectContent(ByteArrayInputStream(json.toByteArray()))
        return Optional.of(s3Object)
    }

    override fun deleteObject(locator: Locator) {
        // noop
    }

    private fun makeKey(locator: Locator, keyStartTime: Long): StoredTemporaryExposureKey {
        return StoredTemporaryExposureKey(locator.key.value, Math.toIntExact(keyStartTime), 144, 7)
    }
}
