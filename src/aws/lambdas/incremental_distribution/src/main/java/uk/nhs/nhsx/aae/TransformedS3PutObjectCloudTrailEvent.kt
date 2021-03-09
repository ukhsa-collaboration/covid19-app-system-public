package uk.nhs.nhsx.aae

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey

data class TransformedS3PutObjectCloudTrailEvent(val bucketName: BucketName, val key: ObjectKey)
