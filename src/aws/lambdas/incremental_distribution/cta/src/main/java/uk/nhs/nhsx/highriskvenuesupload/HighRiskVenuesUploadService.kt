package uk.nhs.nhsx.highriskvenuesupload

import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.signature.DatedSigner
import uk.nhs.nhsx.core.signature.DistributionSignature
import uk.nhs.nhsx.core.signature.SigningHeaders.fromDatedSignature
import uk.nhs.nhsx.highriskvenuesupload.VenuesParsingResult.Failure
import uk.nhs.nhsx.highriskvenuesupload.VenuesParsingResult.Success
import uk.nhs.nhsx.highriskvenuesupload.VenuesUploadResult.Companion.validationError

class HighRiskVenuesUploadService(
    private val config: HighRiskVenuesUploadConfig,
    private val signer: DatedSigner,
    private val s3Client: S3Storage,
    private val awsCloudFront: AwsCloudFront,
    private val parser: HighRiskVenueCsvParser
) {
    fun upload(csv: String?): VenuesUploadResult =
        csv
            ?.let {
                when (val result = parser.toJson(csv)) {
                    is Success -> upload(result)
                    is Failure -> validationError(result.message)
                }
            } ?: validationError("no body")


    private fun upload(result: Success): VenuesUploadResult {
        val bytes = fromUtf8String(result.json)
        val headers = fromDatedSignature(signer.sign(DistributionSignature(bytes)))
        s3Client.upload(config.locator, APPLICATION_JSON, bytes, headers)
        awsCloudFront.invalidateCache(config.cloudFrontDistId, config.cloudFrontInvalidationPattern)
        return VenuesUploadResult.ok()
    }
}
