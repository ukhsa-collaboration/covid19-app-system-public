package uk.nhs.nhsx.core.aws.s3;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.List;
import java.util.Optional;

public interface AwsS3 extends S3Storage {
    List<S3ObjectSummary> getObjectSummaries(BucketName bucketName);

    Optional<S3Object> getObject(Locator locator);

    void deleteObject(Locator locator);
}
