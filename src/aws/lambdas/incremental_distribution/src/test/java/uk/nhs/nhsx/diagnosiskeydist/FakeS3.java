package uk.nhs.nhsx.diagnosiskeydist;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;

public class FakeS3 extends FakeS3Storage implements AwsS3 {

    public List<S3ObjectSummary> existing = newArrayList();

    public List<Map.Entry<BucketName, ObjectKey>> deleted = newArrayList();

    @Override
    public List<S3ObjectSummary> getObjectSummaries(String bucketName) {
        return existing;
    }

    @Override
    public Optional<S3Object> getObject(String bucketName, String key) {
        throw new UnsupportedOperationException("james didn't write");
    }

    @Override
    public void deleteObject(String bucketName, String objectKeyName) {
        deleted.add(new AbstractMap.SimpleEntry<>(BucketName.of(bucketName), ObjectKey.of(objectKeyName)));
    }
}
