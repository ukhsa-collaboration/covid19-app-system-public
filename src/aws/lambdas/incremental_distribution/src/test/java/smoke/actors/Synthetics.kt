package smoke.actors

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig

class Synthetics(private val http: HttpHandler, private val config: EnvConfig) {

    fun checkAnalyticsSubmissionHealth() = getHealthContent(config.analytics_submission_health_endpoint, config.auth_headers.health)

    fun checkDiagnosisKeysSubmissionHealth() = getHealthContent(config.diagnosis_keys_submission_health_endpoint, config.auth_headers.health)

    fun checkEnCircuitBreakerHealth() = getHealthContent(config.exposure_notification_circuit_breaker_health_endpoint, config.auth_headers.health)

    fun checkIsolationPaymentHealth() = getHealthContent(config.isolation_payment_health_endpoint, config.auth_headers.health)

    fun checkRiskyPostDistrictsUploadHealth() = getHealthContent(config.risky_post_districts_upload_health_endpoint, config.auth_headers.health)

    fun checkRiskyVenuesCircuitBreakerHealth() = getHealthContent(config.risky_venues_circuit_breaker_health_endpoint, config.auth_headers.health)

    fun checkRiskyVenuesUploadHealth() = getHealthContent(config.risky_venues_upload_health_endpoint, config.auth_headers.health)

    fun checkTestResultsUploadHealth() = getHealthContent(config.test_results_health_endpoint, config.auth_headers.health)

    fun checkVirologyKitHealth() = getHealthContent(config.virology_kit_health_endpoint, config.auth_headers.health)

    fun checkEmptySubmissionHealth() = getHealthContent(config.empty_submission_endpoint, config.auth_headers.mobile)

    fun checkAnalyticsEventSubmissionHealth() = getHealthContent(config.analytics_events_submission_health_endpoint, config.auth_headers.health)

    private fun getHealthContent(uri: String, authHeader: String): Status = http(Request(Method.POST, uri)
        .header("Authorization", authHeader)).status
}
