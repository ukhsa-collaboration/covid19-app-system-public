package uk.nhs.nhsx.highriskpostcodesupload

import com.amazonaws.services.s3.model.S3Object
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.ContentType.Companion.TEXT_PLAIN
import uk.nhs.nhsx.core.Jackson.readJsonOrThrow
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.signature.DatedSigner
import uk.nhs.nhsx.core.signature.DistributionSignature
import uk.nhs.nhsx.core.signature.SigningHeaders.fromDatedSignature

class RiskyPostCodesPersistence(
    private val bucketName: BucketName,
    private val distributionObjKeyName: ObjectKey,
    private val distributionV2ObjKeyName: ObjectKey,
    private val backupJsonKeyName: ObjectKey,
    private val rawCsvKeyName: ObjectKey,
    private val metaDataObjKeyName: ObjectKey,
    private val signer: DatedSigner,
    private val s3Client: AwsS3
) {
    fun uploadToBackup(json: String) {
        s3Client.upload(
            Locator.of(bucketName, backupJsonKeyName),
            APPLICATION_JSON,
            fromUtf8String(json)
        )
    }

    fun uploadToRaw(csv: String) {
        s3Client.upload(
            Locator.of(bucketName, rawCsvKeyName),
            TEXT_PLAIN,
            fromUtf8String(csv)
        )
    }

    fun uploadPostDistrictsVersion1(riskyPostCodes: String) {
        uploadPostDistrictsVersion(riskyPostCodes, distributionObjKeyName)
    }

    fun uploadPostDistrictsVersion2(riskyPostCodes: String) {
        uploadPostDistrictsVersion(riskyPostCodes, distributionV2ObjKeyName)
    }

    private fun uploadPostDistrictsVersion(riskyPostCodes: String, objectKey: ObjectKey) {
        val byteSource = fromUtf8String(riskyPostCodes)
        s3Client.upload(
            Locator.of(bucketName, objectKey),
            APPLICATION_JSON,
            byteSource,
            fromDatedSignature(signer.sign(DistributionSignature(byteSource)))
        )
    }

    fun retrievePostDistrictRiskLevels(): Map<String, Map<String, Any>> = s3Client
        .getObject(Locator.of(bucketName, metaDataObjKeyName))
        .map(::convertS3ObjectToRiskLevels)
        .orElseThrow {
            RuntimeException("Missing post district metadata. Bucket: ${bucketName.value} does not have key: ${metaDataObjKeyName.value}")
        }

    private fun convertS3ObjectToRiskLevels(s3Object: S3Object): Map<String, Map<String, Any>> =
        s3Object.objectContent.use { readJsonOrThrow(it.reader().readText()) }
}
