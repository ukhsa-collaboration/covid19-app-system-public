@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import software.amazon.awssdk.utils.Md5Utils
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.*

val Md5TemporaryExposureKeyGenerator: (String) -> String =
    { Base64.getEncoder().encodeToString(Md5Utils.computeMD5Hash(it.toByteArray()).copyOf(16)) }


fun exposureS3Object(
    objectKey: String,
    bucketName: BucketName,
    keyValue: String = objectKey
): S3Object {
    val mostRecentKeyRollingStart =
        ENIntervalNumber.enIntervalNumberFromTimestamp(Instant.now()).enIntervalNumber / 144 * 144

    val exposureKey = StoredTemporaryExposureKey(
        key = keyValue,
        rollingStartNumber = Math.toIntExact(mostRecentKeyRollingStart - 144),
        rollingPeriod = 144,
        transmissionRisk = 7
    )

    val json = Json.toJson(StoredTemporaryExposureKeyPayload(listOf(exposureKey)))

    return S3Object().apply {
        this.key = objectKey
        this.bucketName = bucketName.value
        setObjectContent(ByteArrayInputStream(json.toByteArray()))
    }
}
