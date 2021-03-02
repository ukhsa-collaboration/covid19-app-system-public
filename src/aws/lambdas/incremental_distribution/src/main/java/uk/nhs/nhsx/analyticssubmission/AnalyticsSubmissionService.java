package uk.nhs.nhsx.analyticssubmission;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload;
import uk.nhs.nhsx.analyticssubmission.model.StoredAnalyticsSubmissionPayload;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.ByteArraySource;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.events.Events;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AnalyticsSubmissionService {

    private final static Logger log = LogManager.getLogger(AnalyticsSubmissionService.class);

    private final AnalyticsConfig config;
    private final S3Storage s3Storage;
    private final ObjectKeyNameProvider objectKeyNameProvider;
    private final AmazonKinesisFirehose kinesisFirehose;
    private final Events events;

    public AnalyticsSubmissionService(AnalyticsConfig config,
                                      S3Storage s3Storage,
                                      ObjectKeyNameProvider objectKeyNameProvider,
                                      AmazonKinesisFirehose kinesisFirehose,
                                      Events events) {
        this.config = config;
        this.s3Storage = s3Storage;
        this.objectKeyNameProvider = objectKeyNameProvider;
        this.kinesisFirehose = kinesisFirehose;
        this.events = events;
    }

    public void accept(ClientAnalyticsSubmissionPayload payload) {
        String json = Jackson.toJson(StoredAnalyticsSubmissionPayload.convertFrom(payload, events));

        if (config.s3IngestEnabled) {
            uploadToS3(json);
        }

        if (config.firehoseIngestEnabled) {
            uploadToFirehose(json);
        }
    }


    private void uploadToS3(String json) {
        var objectKey = objectKeyNameProvider.generateObjectKeyName().append(".json");

        s3Storage.upload(
            Locator.of(config.bucketName, objectKey),
            ContentType.APPLICATION_JSON,
            ByteArraySource.fromUtf8String(json)
        );
    }

    private void uploadToFirehose(String json) {
        var data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
        var record = new Record().withData(data);

        var putRecordRequest = new PutRecordRequest()
                .withRecord(record)
                .withDeliveryStreamName(config.firehoseStreamName);

        log.info("Sending json to {}", config.firehoseStreamName);

        kinesisFirehose.putRecord(putRecordRequest);
    }
}
