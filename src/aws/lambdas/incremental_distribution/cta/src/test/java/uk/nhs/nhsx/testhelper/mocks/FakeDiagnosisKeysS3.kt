@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import software.amazon.awssdk.utils.Md5Utils
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

val Md5TemporaryExposureKeyGenerator: (String) -> String =
    { Base64.getEncoder().encodeToString(Md5Utils.computeMD5Hash(it.toByteArray()).copyOf(16)) }

class FakeDiagnosisKeysS3(
    private val objectSummaries: List<S3ObjectSummary>,
    private val objectsKeysToSkip: List<String> = listOf(),
    private val keyGenerator: (String) -> String = { it }
) : AwsS3 {

    constructor(vararg summaries: S3ObjectSummary) : this(summaries.toList())

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        metaHeaders: List<MetaHeader>
    ) = Unit

    override fun getObjectSummaries(bucketName: BucketName) = objectSummaries

    override fun getObject(locator: Locator) = when {
        objectsKeysToSkip.contains(locator.key.value) -> Optional.empty()
        else -> {
            val mostRecentKeyRollingStart = ENIntervalNumber
                .enIntervalNumberFromTimestamp(Instant.now())
                .enIntervalNumber / 144 * 144

            val exposureKeyPayload = StoredTemporaryExposureKeyPayload(
                listOf(makeKey(locator, mostRecentKeyRollingStart - 144))
            )

            val json = Json.toJson(exposureKeyPayload)

            Optional.of(S3Object().apply {
                setObjectContent(ByteArrayInputStream(json.toByteArray()))
            })
        }
    }

    override fun deleteObject(locator: Locator) = Unit
    override fun copyObject(from: Locator, to: Locator) = Unit
    override fun getSignedURL(locator: Locator, expiration: Date) = Optional.of(URL("https://example.com"))
    private fun makeKey(locator: Locator, keyStartTime: Long) = StoredTemporaryExposureKey(
        keyGenerator(locator.key.value),
        Math.toIntExact(keyStartTime),
        144,
        7
    )
}
