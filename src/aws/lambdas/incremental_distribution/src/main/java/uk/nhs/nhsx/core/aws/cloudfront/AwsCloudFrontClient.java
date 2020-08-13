package uk.nhs.nhsx.core.aws.cloudfront;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.AmazonCloudFrontException;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Supplier;

public class AwsCloudFrontClient implements AwsCloudFront {

    private static final Logger logger = LoggerFactory.getLogger(AwsCloudFrontClient.class);

    private static final Supplier<AmazonCloudFront> client =
        Suppliers.memoize(AmazonCloudFrontClientBuilder::defaultClient);

    @Override
    public void invalidateCache(String distributionId, String path) {
        Paths invalidationPaths = new Paths().withItems(path).withQuantity(1);
        InvalidationBatch invalidationBatch = new InvalidationBatch(invalidationPaths, UUID.randomUUID().toString());
        CreateInvalidationRequest invalidationRequest = new CreateInvalidationRequest(distributionId, invalidationBatch);

        try {
            client.get().createInvalidation(invalidationRequest);
        } catch (AmazonCloudFrontException e) {
            logger.error("CloudFront cache invalidation failed", e);
        }
    }
}
