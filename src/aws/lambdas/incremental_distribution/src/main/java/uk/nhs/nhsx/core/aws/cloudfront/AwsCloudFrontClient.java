package uk.nhs.nhsx.core.aws.cloudfront;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.AmazonCloudFrontException;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class AwsCloudFrontClient implements AwsCloudFront {

    private static final Logger logger = LogManager.getLogger(AwsCloudFrontClient.class);

    private final AmazonCloudFront client;

    public AwsCloudFrontClient() {
        client = AmazonCloudFrontClientBuilder.defaultClient();
    }

    @Override
    public void invalidateCache(String distributionId, String path) {
        Paths invalidationPaths = new Paths().withItems(path).withQuantity(1);
        InvalidationBatch invalidationBatch = new InvalidationBatch(invalidationPaths, UUID.randomUUID().toString());
        CreateInvalidationRequest invalidationRequest = new CreateInvalidationRequest(distributionId, invalidationBatch);

        try {
            client.createInvalidation(invalidationRequest);
        } catch (AmazonCloudFrontException e) {
            logger.error("CloudFront cache invalidation failed", e);
        }
    }
}
