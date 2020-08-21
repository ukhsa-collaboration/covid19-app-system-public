package uk.nhs.nhsx.core.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.google.common.base.Suppliers;
import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Arrays.stream;

public class AwsS3Client implements AwsS3 {

    private static final Logger logger = LoggerFactory.getLogger(AwsS3Client.class);

    private static final Supplier<AmazonS3> client =
        Suppliers.memoize(AmazonS3ClientBuilder::defaultClient);

    @Override
    public void upload(Locator locator, ContentType contentType, ByteSource bytes, MetaHeader... meta) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType.getMimeType());
        stream(meta).forEach(m -> metadata.addUserMetadata(m.asS3MetaName(), m.value));
        bytes.sizeIfKnown().toJavaUtil().ifPresent(metadata::setContentLength);
        try (InputStream input = bytes.openBufferedStream()) {
            client.get().putObject(
                new PutObjectRequest(locator.bucket.value, locator.key.value, input, metadata)
            );
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to open data source %s", bytes), e);
        }
    }

    @Override
    public List<S3ObjectSummary> getObjectSummaries(String bucketName) {
        List<S3ObjectSummary> objectSummaries = new ArrayList<>();

        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
        listObjectsV2Request.setBucketName(bucketName);

        ListObjectsV2Result result = client.get().listObjectsV2(listObjectsV2Request);
        objectSummaries.addAll(result.getObjectSummaries());

        while (result.isTruncated()) {
            String token = result.getNextContinuationToken();
            listObjectsV2Request.setContinuationToken(token);

            result = client.get().listObjectsV2(listObjectsV2Request);
            objectSummaries.addAll(result.getObjectSummaries());
        }

        return objectSummaries;
    }

    @Override
    public Optional<S3Object> getObject(String bucketName, String objectKey) {
        try {
            return Optional.ofNullable(client.get().getObject(bucketName, objectKey));
        } catch (AmazonS3Exception e) {
            if (!(e.getStatusCode() == 404 && Objects.equals(e.getErrorCode(), "NoSuchKey"))) {
                logger.warn("Object could not be retrieved", e);
            }
            return Optional.empty();
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectKey) {
        client.get().deleteObject(bucketName, objectKey);
    }

}
