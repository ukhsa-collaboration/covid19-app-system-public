package uk.nhs.nhsx.circuitbreakers;

import static java.util.Objects.requireNonNull;

public class ExposureNotificationCircuitBreakerRequest {

    public Integer matchedKeyCount;
    public Integer daysSinceLastExposure;
    public Double maximumRiskScore;
    public Integer riskCalculationVersion = 1;

    public static ExposureNotificationCircuitBreakerRequest validate(ExposureNotificationCircuitBreakerRequest request) {
        requireNonNull(request.matchedKeyCount);
        requireNonNull(request.daysSinceLastExposure);
        requireNonNull(request.maximumRiskScore);
        requireNonNull(request.riskCalculationVersion);
        return request;
    }
}
