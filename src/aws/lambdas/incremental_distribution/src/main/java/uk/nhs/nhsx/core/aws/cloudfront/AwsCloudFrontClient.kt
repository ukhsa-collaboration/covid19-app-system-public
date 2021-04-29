package uk.nhs.nhsx.core.aws.cloudfront

import com.amazonaws.services.cloudfront.AmazonCloudFront
import com.amazonaws.services.cloudfront.model.AmazonCloudFrontException
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest
import com.amazonaws.services.cloudfront.model.InvalidationBatch
import com.amazonaws.services.cloudfront.model.Paths
import uk.nhs.nhsx.core.events.Events
import java.util.*

class AwsCloudFrontClient(
    private val events: Events,
    private val client: AmazonCloudFront
) : AwsCloudFront {

    override fun invalidateCache(distributionId: String, path: String) {
        val batchId = UUID.randomUUID()
        val invalidationPaths = Paths().withItems(path).withQuantity(1)
        val invalidationBatch = InvalidationBatch(invalidationPaths, batchId.toString())
        val invalidationRequest = CreateInvalidationRequest(distributionId, invalidationBatch)
        try {
            client.createInvalidation(invalidationRequest)
            events(CloudFrontCachesInvalidated(distributionId, path, batchId))
        } catch (e: AmazonCloudFrontException) {
            events(CloudFrontCacheInvalidationFailed(distributionId, path, batchId, e))
        }
    }
}
