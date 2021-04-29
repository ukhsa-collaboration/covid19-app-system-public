package uk.nhs.nhsx.core.aws.s3

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning

data class S3Error(
    val locator: Locator,
    val statusCode: Int,
    val errorCode: String
) : Event(Warning)

data class S3Upload(
    val objectKey: ObjectKey,
    val bucket: BucketName
): Event(Info)
