package uk.nhs.nhsx.analyticssubmission;

import uk.nhs.nhsx.core.aws.s3.BucketName;

public class AnalyticsConfig {

    public final  String firehoseStreamName;
    public final  boolean s3IngestEnabled;
    public final  boolean firehoseIngestEnabled;
    public final BucketName bucketName;

    public AnalyticsConfig(String firehoseStreamName,
                           boolean s3IngestEnabled,
                           boolean firehoseIngestEnabled,
                           String bucketName) {
        this.firehoseStreamName = firehoseStreamName;
        this.s3IngestEnabled = s3IngestEnabled;
        this.firehoseIngestEnabled = firehoseIngestEnabled;
        this.bucketName = BucketName.of(bucketName);
    }
}
