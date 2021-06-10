package uk.nhs.nhsx.analyticssubmission.model

data class ClientAnalyticsSubmissionPayload(
    @JvmField val analyticsWindow: AnalyticsWindow,
    @JvmField val metadata: AnalyticsMetadata,
    @JvmField val metrics: AnalyticsMetrics,
    @JvmField val includesMultipleApplicationVersions: Boolean
)
