package uk.nhs.nhsx.analyticsevents;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.analyticssubmission.PostCodeDeserializer;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.aws.s3.Sources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AnalyticsEventsSubmissionService {

    private final static Logger log = LogManager.getLogger(AnalyticsEventsSubmissionService.class);

    private final S3Storage s3Storage;
    private final ObjectKeyNameProvider objectKeyNameProvider;
    private final BucketName bucketName;

    public AnalyticsEventsSubmissionService(S3Storage s3Storage, ObjectKeyNameProvider objectKeyNameProvider, BucketName bucketName) {
        this.s3Storage = s3Storage;
        this.objectKeyNameProvider = objectKeyNameProvider;
        this.bucketName = bucketName;
    }

    public void accept(Map<String, Object> payload) {
        var storedPayload = transformPayload(payload);
        String json = Jackson.toJson(storedPayload);
        uploadToS3(json);
    }

    private Map<String, Object> transformPayload(Map<String, Object> payload) {
        Object metadataRaw = payload.get("metadata");

        if (!(metadataRaw instanceof Map)) {
            throw new RuntimeException("metadata must be a map");
        }

        var metadata = (Map<?, ?>) metadataRaw;
        var currentPostalDistrict = (String) metadata.get("postalDistrict");

        var transformedMetadata = new LinkedHashMap<>();
        transformedMetadata.putAll(metadata);
        transformedMetadata.put("postalDistrict", PostCodeDeserializer.mergeSmallPostcodes(currentPostalDistrict));
        transformedMetadata.remove("localAuthority");

        var transformedPayload = new LinkedHashMap<>(payload);
        transformedPayload.put("uuid", UUID.randomUUID());
        transformedPayload.put("metadata", transformedMetadata);

        return transformedPayload;
    }

    private void uploadToS3(String json) {
        var objectKey = objectKeyNameProvider.generateObjectKeyName().append(".json");
        log.info("Uploading {} to {}", objectKey, bucketName.value);

        s3Storage.upload(
            S3Storage.Locator.of(bucketName, objectKey),
            ContentType.APPLICATION_JSON,
            Sources.byteSourceFor(json)
        );
    }

}
