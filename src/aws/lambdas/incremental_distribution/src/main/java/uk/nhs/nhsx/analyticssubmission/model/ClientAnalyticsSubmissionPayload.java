package uk.nhs.nhsx.analyticssubmission.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ClientAnalyticsSubmissionPayload {

    public final AnalyticsWindow analyticsWindow;
    public final AnalyticsMetadata metadata;
    public final AnalyticsMetrics metrics;
    public final boolean includesMultipleApplicationVersions;

    @JsonCreator
    public ClientAnalyticsSubmissionPayload(AnalyticsWindow analyticsWindow,
                                            AnalyticsMetadata metadata,
                                            AnalyticsMetrics metrics,
                                            boolean includesMultipleApplicationVersions) {
        this.analyticsWindow = analyticsWindow;
        this.metadata = metadata;
        this.metrics = metrics;
        this.includesMultipleApplicationVersions = includesMultipleApplicationVersions;
    }
}