package uk.nhs.nhsx.analyticsedge

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory

data class DataUploadedToEdge(
    val sqsMessageId: String?,
    val bucketName: BucketName,
    val key: ObjectKey
) : Event(EventCategory.Info)

data class DataUploadSkipped(
    val sqsMessageId: String?,
    val bucketName: BucketName,
    val key: ObjectKey
) : Event(EventCategory.Info)
