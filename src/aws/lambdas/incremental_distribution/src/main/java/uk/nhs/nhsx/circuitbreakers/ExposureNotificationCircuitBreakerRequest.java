package uk.nhs.nhsx.circuitbreakers;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public class ExposureNotificationCircuitBreakerRequest {

    public final int matchedKeyCount;
    public final int daysSinceLastExposure;
    public final double maximumRiskScore;
    public final int riskCalculationVersion;

    @JsonCreator
    public ExposureNotificationCircuitBreakerRequest(int matchedKeyCount, int daysSinceLastExposure, double maximumRiskScore) {
        this.matchedKeyCount = matchedKeyCount;
        this.daysSinceLastExposure = daysSinceLastExposure;
        this.maximumRiskScore = maximumRiskScore;
        this.riskCalculationVersion = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExposureNotificationCircuitBreakerRequest that = (ExposureNotificationCircuitBreakerRequest) o;
        return matchedKeyCount == that.matchedKeyCount && daysSinceLastExposure == that.daysSinceLastExposure
            && Double.compare(that.maximumRiskScore, maximumRiskScore) == 0 && riskCalculationVersion == that.riskCalculationVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchedKeyCount, daysSinceLastExposure, maximumRiskScore, riskCalculationVersion);
    }

    @Override
    public String toString() {
        return "ExposureNotificationCircuitBreakerRequest{" +
            "matchedKeyCount=" + matchedKeyCount +
            ", daysSinceLastExposure=" + daysSinceLastExposure +
            ", maximumRiskScore=" + maximumRiskScore +
            ", riskCalculationVersion=" + riskCalculationVersion +
            '}';
    }
}
