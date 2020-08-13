package uk.nhs.nhsx.analyticssubmission;

import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload;
import uk.nhs.nhsx.analyticssubmission.model.StoredAnalyticsSubmissionPayload;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.*;

public class AnalyticsSubmissionService {

    private final BucketName bucketName;
    private final S3Storage s3Storage;
    private final ObjectKeyNameProvider objectKeyNameProvider;

    public AnalyticsSubmissionService(String bucketName, S3Storage s3Storage, ObjectKeyNameProvider objectKeyNameProvider) {
        this.bucketName = BucketName.of(bucketName);
        this.s3Storage = s3Storage;
        this.objectKeyNameProvider = objectKeyNameProvider;
    }

    public void accept(ClientAnalyticsSubmissionPayload payload) {
        ObjectKey objectKey = objectKeyNameProvider.generateObjectKeyName().append(".json");
        String json = Jackson.toJson(StoredAnalyticsSubmissionPayload.convertFrom(payload));
        s3Storage.upload(S3Storage.Locator.of(bucketName, objectKey), ContentType.APPLICATION_JSON, Sources.byteSourceFor(json));
    }
}
