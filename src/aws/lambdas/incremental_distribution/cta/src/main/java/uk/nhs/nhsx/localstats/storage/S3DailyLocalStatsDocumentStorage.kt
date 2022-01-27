package uk.nhs.nhsx.localstats.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.signature.DatedSigner
import uk.nhs.nhsx.core.signature.DistributionSignature
import uk.nhs.nhsx.core.signature.SigningHeaders
import uk.nhs.nhsx.localstats.LocalStatsJson
import uk.nhs.nhsx.localstats.domain.DailyLocalStatsDocument
import java.time.Instant
import java.util.*

class S3DailyLocalStatsDocumentStorage(
    private val bucketName: BucketName,
    private val amazonS3: AmazonS3,
    private val signer: DatedSigner
) : DailyLocalStatsDocumentStorage {

    private val objectKey = "distribution/v1/local-covid-stats-daily"

    override fun exists() = amazonS3.doesObjectExist(bucketName.value, objectKey)

    override fun lastModified(): Instant = amazonS3
        .getObjectMetadata(bucketName.value, objectKey)
        .lastModified
        .let(Date::toInstant)

    override fun put(document: DailyLocalStatsDocument) {
        val content = LocalStatsJson.asFormatString(document)
        val byteSource = ByteArraySource.fromUtf8String(content)
        val sigHeaders = SigningHeaders.fromDatedSignature(signer.sign(DistributionSignature(byteSource)))
        val metadata = ObjectMetadata().apply {
            contentType = "application/json"
            contentLength = byteSource.size.toLong()
            sigHeaders.forEach { addUserMetadata(it.asS3MetaName(), it.value) }
        }
        byteSource.openStream().use {
            amazonS3.putObject(PutObjectRequest(bucketName.value, objectKey, it, metadata))
        }
    }
}
