package uk.nhs.nhsx.core.aws.s3;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.List;
import java.util.Optional;

public interface AwsS3 extends S3Storage {
    List<S3ObjectSummary> getObjectSummaries(String bucketName);
    Optional<S3Object> getObject(String bucketName, String key);
    void deleteObject(String bucketName, String objectKeyName);
}
