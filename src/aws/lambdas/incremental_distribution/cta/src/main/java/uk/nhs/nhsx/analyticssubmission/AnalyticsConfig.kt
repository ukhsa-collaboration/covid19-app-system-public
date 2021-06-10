package uk.nhs.nhsx.analyticssubmission

import uk.nhs.nhsx.core.aws.s3.BucketName

data class AnalyticsConfig(
    val firehoseStreamName: String,
    val s3IngestEnabled: Boolean,
    val firehoseIngestEnabled: Boolean,
    val bucketName: BucketName
)
