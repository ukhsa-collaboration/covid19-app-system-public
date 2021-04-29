package uk.nhs.nhsx.core.aws.s3

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import java.util.Optional

interface AwsS3 : S3Storage {
    fun getObjectSummaries(bucketName: BucketName): List<S3ObjectSummary>
    fun getObject(locator: Locator): Optional<S3Object>
    fun deleteObject(locator: Locator)
    fun copyObject(from: Locator, to: Locator)
}
