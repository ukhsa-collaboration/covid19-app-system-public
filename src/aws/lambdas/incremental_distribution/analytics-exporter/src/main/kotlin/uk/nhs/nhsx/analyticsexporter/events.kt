package uk.nhs.nhsx.analyticsexporter

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Error
import uk.nhs.nhsx.core.events.EventCategory.Info

data class S3ToParquetObjectConversionFailure(
    val sqsMessageId: String?,
    val bucketName: BucketName,
    val key: ObjectKey
) : Event(Error)

data class S3ObjectNotFound(val sqsMessageId: String?, val bucketName: BucketName, val key: ObjectKey) : Event(Error)

data class DataUploadedToS3(val sqsMessageId: String?, val bucketName: BucketName, val key: ObjectKey) : Event(Info)
