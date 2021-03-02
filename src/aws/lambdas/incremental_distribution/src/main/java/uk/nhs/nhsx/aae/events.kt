package uk.nhs.nhsx.aae

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.EventCategory.Info

data class S3ToParquetObjectConversionFailure(
    val sqsMessageId: String?,
    val bucketName: String?,
    val key: String?
) : Event(EventCategory.Error)

data class S3ObjectNotFound(
    val sqsMessageId: String?,
    val bucketName: String?,
    val key: String?
) : Event(EventCategory.Error)

data class AAEDataUploadedToS3(val sqsMessageId: String?, val bucketName: String?, val key: String?) : Event(Info)
