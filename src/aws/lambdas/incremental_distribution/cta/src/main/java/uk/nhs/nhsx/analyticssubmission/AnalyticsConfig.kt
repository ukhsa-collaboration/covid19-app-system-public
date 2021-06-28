package uk.nhs.nhsx.analyticssubmission

data class AnalyticsConfig(
    val firehoseStreamName: String,
    val firehoseIngestEnabled: Boolean
)
