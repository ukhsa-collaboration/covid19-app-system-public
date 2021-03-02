package smoke.actors

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig

class Synthetics(private val http: HttpHandler, private val config: EnvConfig) {

    fun checkAnalyticsSubmissionHealth() = getHealthContent(config.analyticsSubmissionHealthEndpoint, config.authHeaders.health)

    fun checkDiagnosisKeysSubmissionHealth() = getHealthContent(config.diagnosisKeysSubmissionHealthEndpoint, config.authHeaders.health)

    fun checkEnCircuitBreakerHealth() = getHealthContent(config.enCircuitBreakerHealthEndpoint, config.authHeaders.health)

    fun checkIsolationPaymentHealth() = getHealthContent(config.isolationPaymentHealthEndpoint, config.authHeaders.health)

    fun checkRiskyPostDistrictsUploadHealth() = getHealthContent(config.riskyPostDistrictsUploadHealthEndpoint, config.authHeaders.health)

    fun checkRiskyVenuesCircuitBreakerHealth() = getHealthContent(config.riskyVenuesCircuitBreakerHealthEndpoint, config.authHeaders.health)

    fun checkRiskyVenuesUploadHealth() = getHealthContent(config.riskyVenuesUploadHealthEndpoint, config.authHeaders.health)

    fun checkTestResultsUploadHealth() = getHealthContent(config.testResultsHealthEndpoint, config.authHeaders.health)

    fun checkVirologyKitHealth() = getHealthContent(config.virologyKitHealthEndpoint, config.authHeaders.health)

    fun checkEmptySubmissionHealth() = getHealthContent(config.emptySubmissionEndpoint, config.authHeaders.mobile)

    fun checkAnalyticsEventSubmissionHealth() = getHealthContent(config.analyticsEventsSubmissionHealthEndpoint, config.authHeaders.health)

    private fun getHealthContent(uri: String, authHeader: String): Status = http(Request(Method.POST, uri)
        .header("Authorization", authHeader)).status
}
