package uk.nhs.nhsx.aae

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

data class S3ParquetFileNotCreatedByFirehouse(
    val sqsMessageId: String?,
    val bucketName: BucketName,
    val key: ObjectKey
) : Event(Error)

data class S3ObjectStartsWithDisallowedPrefix(
    val sqsMessageId: String?,
    val bucketName: BucketName,
    val key: ObjectKey
) : Event(Error)

data class S3ObjectNotFound(val sqsMessageId: String?, val bucketName: BucketName, val key: ObjectKey) : Event(Error)

data class DataUploadedToAAE(val sqsMessageId: String?, val bucketName: BucketName, val key: ObjectKey) : Event(Info)
