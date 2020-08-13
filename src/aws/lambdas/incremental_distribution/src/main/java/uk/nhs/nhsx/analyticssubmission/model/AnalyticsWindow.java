package uk.nhs.nhsx.analyticssubmission.model;

import uk.nhs.nhsx.core.DateFormatValidator;

public class AnalyticsWindow {
    
    public final String startDate;
    public final String endDate;

    public AnalyticsWindow(String startDate, String endDate) {
        DateFormatValidator
            .toZonedDateTimeMaybe(startDate)
            .orElseThrow(() -> new IllegalArgumentException("Invalid startDate format"));
        DateFormatValidator
            .toZonedDateTimeMaybe(endDate)
            .orElseThrow(() -> new IllegalArgumentException("Invalid endDate format"));
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
