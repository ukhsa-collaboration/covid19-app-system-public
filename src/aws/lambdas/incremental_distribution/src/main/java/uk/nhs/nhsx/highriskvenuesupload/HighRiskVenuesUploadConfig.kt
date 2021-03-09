package uk.nhs.nhsx.highriskvenuesupload

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey

data class HighRiskVenuesUploadConfig(
    val locator: Locator,
    val cloudFrontDistId: String,
    val cloudFrontInvalidationPattern: String,
) {
    companion object {
        fun of(
            bucket: BucketName,
            key: ObjectKey,
            cloudFrontDistId: String,
            cloudFrontInvalidationPattern: String,
        ) = HighRiskVenuesUploadConfig(Locator.of(bucket, key), cloudFrontDistId, cloudFrontInvalidationPattern)
    }
}
