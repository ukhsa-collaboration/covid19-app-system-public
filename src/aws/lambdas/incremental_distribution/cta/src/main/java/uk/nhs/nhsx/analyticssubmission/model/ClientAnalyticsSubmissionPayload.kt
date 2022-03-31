package uk.nhs.nhsx.analyticssubmission.model

data class ClientAnalyticsSubmissionPayload(
    val analyticsWindow: AnalyticsWindow,
    val metadata: AnalyticsMetadata,
    val metrics: AnalyticsMetrics,
    val includesMultipleApplicationVersions: Boolean
)
