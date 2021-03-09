package uk.nhs.nhsx.core.aws.cloudfront

import com.amazonaws.services.cloudfront.AmazonCloudFront
import com.amazonaws.services.cloudfront.model.AmazonCloudFrontException
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest
import com.amazonaws.services.cloudfront.model.InvalidationBatch
import com.amazonaws.services.cloudfront.model.Paths
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown
import java.util.*

class AwsCloudFrontClient(
    private val events: Events,
    private val client: AmazonCloudFront
) : AwsCloudFront {

    override fun invalidateCache(distributionId: String, path: String) {
        val invalidationPaths = Paths().withItems(path).withQuantity(1)
        val invalidationBatch = InvalidationBatch(invalidationPaths, UUID.randomUUID().toString())
        val invalidationRequest = CreateInvalidationRequest(distributionId, invalidationBatch)
        try {
            client.createInvalidation(invalidationRequest)
        } catch (e: AmazonCloudFrontException) {
            events(javaClass, ExceptionThrown(e, "CloudFront cache invalidation failed"))
        }
    }
}
