package uk.nhs.nhsx.core.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.stream;

public class AwsS3Client implements AwsS3 {

    private static final Logger logger = LogManager.getLogger(AwsS3Client.class);

    private final AmazonS3 client;

    public AwsS3Client() {
        client = AmazonS3ClientBuilder.defaultClient();
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void upload(Locator locator, ContentType contentType, ByteSource bytes, MetaHeader... meta) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType.getMimeType());
        stream(meta).forEach(m -> metadata.addUserMetadata(m.asS3MetaName(), m.value));
        bytes.sizeIfKnown().toJavaUtil().ifPresent(metadata::setContentLength);
        try (InputStream input = bytes.openBufferedStream()) {
            client.putObject(
                new PutObjectRequest(locator.bucket.value, locator.key.value, input, metadata)
            );
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to open data source %s", bytes), e);
        }
    }

    @Override
    public List<S3ObjectSummary> getObjectSummaries(BucketName bucketName) {
        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
        listObjectsV2Request.setBucketName(bucketName.value);

        ListObjectsV2Result result = client.listObjectsV2(listObjectsV2Request);
        List<S3ObjectSummary> objectSummaries = new ArrayList<>(result.getObjectSummaries());

        while (result.isTruncated()) {
            String token = result.getNextContinuationToken();
            listObjectsV2Request.setContinuationToken(token);

            result = client.listObjectsV2(listObjectsV2Request);
            objectSummaries.addAll(result.getObjectSummaries());
        }

        return objectSummaries;
    }

    @Override
    public Optional<S3Object> getObject(Locator locator) {
        try {
            return Optional.ofNullable(client.getObject(locator.bucket.value, locator.key.value));
        } catch (AmazonS3Exception e) {
            if (!(e.getStatusCode() == 404 && Objects.equals(e.getErrorCode(), "NoSuchKey"))) {
                logger.warn("Object could not be retrieved", e);
            }
            return Optional.empty();
        }
    }

    @Override
    public void deleteObject(Locator locator) {
        client.deleteObject(locator.bucket.value, locator.key.value);
    }

}
