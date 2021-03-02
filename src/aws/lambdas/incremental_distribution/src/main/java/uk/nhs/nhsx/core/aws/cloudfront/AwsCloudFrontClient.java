package uk.nhs.nhsx.core.aws.cloudfront;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.AmazonCloudFrontException;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.ExceptionThrown;

import java.util.UUID;

public class AwsCloudFrontClient implements AwsCloudFront {

    private final AmazonCloudFront client;
    private final Events events;

    public AwsCloudFrontClient(Events events) {
        this.events = events;
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
            events.emit(getClass(), new ExceptionThrown<>(e, "CloudFront cache invalidation failed"));
        }
    }
}
