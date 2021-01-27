package uk.nhs.nhsx.aae;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.aws.s3.*;

import java.util.Optional;

/**
 * S3 PutObject -> CloudTrail -> EventBridge rule & transformation -> SQS -> Lambda: Upload of S3 object (e.g. JSON, Parquet) to AAE via HTTPS PUT
 */
public class AAEUploadHandler implements RequestHandler<SQSEvent, String> {
    private static final Logger logger = LogManager.getLogger(AAEUploadHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FORMAT_CONVERSION_FAILED_PREFIX = "format-conversion-failed/";

    private final AwsS3 s3Client;
    private final AAEUploader aaeUploader;

    @SuppressWarnings("unused")
    public AAEUploadHandler() {
        this(new AwsS3Client(), new AAEUploader());
    }

    public AAEUploadHandler(AwsS3 s3Client, AAEUploader aaeUploader) {
        this.s3Client = s3Client;
        this.aaeUploader = aaeUploader;
    }

    public String handleRequest(SQSEvent input, Context context) {
        if (input.getRecords().size() != 1) {
            logger.error(".tf configuration error: batch_size != 1");
            throw new IllegalStateException();
        }

        SQSEvent.SQSMessage sqsMessage = input.getRecords().get(0);

        logger.debug("Processing SQS message: sqsMessage.id={}, attributes={}, body={}", sqsMessage.getMessageId(), sqsMessage.getAttributes(), sqsMessage.getBody());

        TransformedS3PutObjectCloudTrailEvent event;
        try {
            event = objectMapper.readValue(sqsMessage.getBody(), TransformedS3PutObjectCloudTrailEvent.class);
            if (event.getKey() == null) throw new RuntimeException("missing: key");
            if (event.getBucketName() == null) throw new RuntimeException("missing: bucketName");
        }
        catch (Exception e) { // -> no retry
            logger.error("SQS message parsing failed (no retry): sqsMessage.id={}, body={}", sqsMessage.getMessageId(), sqsMessage.getBody(), e);
            return "parsing-error";
        }

        if (event.getKey().startsWith(FORMAT_CONVERSION_FAILED_PREFIX)) {
            logger.error("S3 object failed conversion to parquet: sqsMessage.id={}, bucketName={}, key={}", sqsMessage.getMessageId(), event.getBucketName(), event.getKey());
            return "format-conversion-error";
        }

        try {
            BucketName bucketName = BucketName.of(event.getBucketName());
            ObjectKey key = ObjectKey.of(event.getKey());
            Optional<S3Object> s3Object = s3Client.getObject(Locator.of(bucketName, key));

            if (s3Object.isEmpty()) {
                logger.warn("S3 object not found: sqsMessage.id={}, bucketName={}, key={}", sqsMessage.getMessageId(), event.getBucketName(), event.getKey());
                return "not-found";
            }

            aaeUploader.uploadToAAE(s3Object.get());

            logger.info("S3 object successfully uploaded to AAE: sqsMessage.id={}, bucketName={}, key={}", sqsMessage.getMessageId(), event.getBucketName(), event.getKey());
            return "success";
        }
        catch (Exception e) { // -> retry or DLQ
            logger.warn("S3 object NOT uploaded to AAE (retry candidate): sqsMessage.id={}, bucketName={}, key={}", sqsMessage.getMessageId(), event.getBucketName(), event.getKey(), e);
            throw new RuntimeException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class TransformedS3PutObjectCloudTrailEvent {
        private String bucketName;
        private String key;

        public String getBucketName() {
            return bucketName;
        }
        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
        public String getKey() {
            return key;
        }
        public void setKey(String key) {
            this.key = key;
        }
    }
}
