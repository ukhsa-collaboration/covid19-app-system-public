package uk.nhs.nhsx.aae;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.ExceptionThrown;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.core.queued.Queued;
import uk.nhs.nhsx.core.queued.QueuedHandler;

import java.util.Optional;

import static uk.nhs.nhsx.core.SystemClock.CLOCK;

/**
 * S3 PutObject -> CloudTrail -> EventBridge rule & transformation -> SQS -> Lambda: Upload of S3 object (e.g. JSON, Parquet) to AAE via HTTPS PUT
 */
public class AAEUploadHandler extends QueuedHandler {
    private static final String FORMAT_CONVERSION_FAILED_PREFIX = "format-conversion-failed/";

    private final Queued.Handler handler;

    @SuppressWarnings("unused")
    public AAEUploadHandler() {
        this(
            new AwsS3Client(new PrintingJsonEvents(CLOCK)),
            new AAEUploader(AAEUploadConfig.fromEnvironment(Environment.fromSystem()), new AwsSecretManager(), new PrintingJsonEvents(CLOCK)),
            new PrintingJsonEvents(CLOCK)
        );
    }

    public AAEUploadHandler(AwsS3 s3Client, AAEUploader aaeUploader, Events events) {
        super(events);

        handler = (input, context) -> {
            if (input.getRecords().size() != 1) {
                ExceptionThrown<RuntimeException> event = new ExceptionThrown<>(new IllegalStateException(".tf configuration error: batch_size != 1"));
                events.emit(getClass(), event);
                throw event.getException();
            }

            SQSEvent.SQSMessage sqsMessage = input.getRecords().get(0);

            TransformedS3PutObjectCloudTrailEvent event;
            try {
                event = Jackson.readJson(sqsMessage.getBody(), TransformedS3PutObjectCloudTrailEvent.class);
                if (event.getKey() == null) throw new RuntimeException("missing: key");
                if (event.getBucketName() == null) throw new RuntimeException("missing: bucketName");
            } catch (Exception e) { // -> no retry
                return new ExceptionThrown<>(new RuntimeException("SQS message parsing failed (no retry): sqsMessage.id=" + sqsMessage.getMessageId() + ", body=" + sqsMessage.getBody(), e));
            }

            if (event.getKey().startsWith(FORMAT_CONVERSION_FAILED_PREFIX)) {
                return new S3ToParquetObjectConversionFailure(sqsMessage.getMessageId(), event.getBucketName(), event.getKey());
            }

            try {
                BucketName bucketName = BucketName.of(event.getBucketName());
                ObjectKey key = ObjectKey.of(event.getKey());
                Optional<S3Object> s3Object = s3Client.getObject(Locator.of(bucketName, key));

                if (s3Object.isEmpty()) {
                    return new S3ObjectNotFound(sqsMessage.getMessageId(), event.getBucketName(), event.getKey());
                }

                aaeUploader.uploadToAAE(s3Object.get());

                return new AAEDataUploadedToS3(sqsMessage.getMessageId(), event.getBucketName(), event.getKey());
            } catch (Exception e) { // -> retry o
                throw new RuntimeException("S3 object NOT uploaded to AAE (retry candidate): sqsMessage.id=" + sqsMessage.getMessageId() + ", body=" + sqsMessage.getBody(), e);// r DLQ
            }
        };
    }

    @NotNull
    @Override
    public Queued.Handler handler() {
        return handler;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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
