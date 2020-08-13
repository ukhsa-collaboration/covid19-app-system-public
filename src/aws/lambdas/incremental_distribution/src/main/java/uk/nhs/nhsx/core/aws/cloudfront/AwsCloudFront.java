package uk.nhs.nhsx.core.aws.cloudfront;

public interface AwsCloudFront {
    void invalidateCache(String distributionId, String path);
}
