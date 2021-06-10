package uk.nhs.nhsx.core.aws.cloudfront

import com.amazonaws.services.cloudfront.model.AmazonCloudFrontException
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Error
import uk.nhs.nhsx.core.events.EventCategory.Info
import java.util.*

data class CloudFrontCachesInvalidated(
    val distributionId: String,
    val path: String,
    val invalidationBatch: UUID
) : Event(Info)

data class CloudFrontCacheInvalidationFailed(
    val distributionId: String,
    val path: String,
    val invalidationBatch: UUID,
    val exception: AmazonCloudFrontException,
    val message: String = exception.localizedMessage ?: "<missing>"
) : Event(Error)
