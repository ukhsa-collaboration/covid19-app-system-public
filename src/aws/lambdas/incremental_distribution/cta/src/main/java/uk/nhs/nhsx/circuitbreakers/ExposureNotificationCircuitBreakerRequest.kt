package uk.nhs.nhsx.circuitbreakers

data class ExposureNotificationCircuitBreakerRequest(
    val matchedKeyCount: Int,
    val daysSinceLastExposure: Int,
    val maximumRiskScore: Double
) {
    val riskCalculationVersion = 1
}
