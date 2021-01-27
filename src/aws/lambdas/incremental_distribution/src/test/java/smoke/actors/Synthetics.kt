package smoke.actors

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig

class Synthetics(private val http: HttpHandler, private val config: EnvConfig) {

    fun checkAnalyticsSubmissionHealth() = getHealthContent(config.analyticsSubmissionHealthEndpoint, config.authHeaders.mobile)

    fun checkDiagnosisKeysSubmissionHealth() = getHealthContent(config.diagnosisKeysSubmissionHealthEndpoint, config.authHeaders.mobile)

    fun checkEnCircuitBreakerHealth() = getHealthContent(config.enCircuitBreakerHealthEndpoint, config.authHeaders.mobile)

    fun checkIsolationPaymentHealth() = getHealthContent(config.isolationPaymentHealthEndpoint, config.authHeaders.mobile)

    fun checkRiskyPostDistrictsUploadHealth() = getHealthContent(config.riskyPostDistrictsUploadHealthEndpoint, config.authHeaders.highRiskPostCodeUpload)

    fun checkRiskyVenuesCircuitBreakerHealth() = getHealthContent(config.riskyVenuesCircuitBreakerHealthEndpoint, config.authHeaders.mobile)

    fun checkRiskyVenuesUploadHealth() = getHealthContent(config.riskyVenuesUploadHealthEndpoint, config.authHeaders.highRiskVenuesCodeUpload)

    fun checkTestResultsUploadHealth() = getHealthContent(config.testResultsHealthEndpoint, config.authHeaders.testResultUpload)

    fun checkVirologyKitHealth() = getHealthContent(config.virologyKitHealthEndpoint, config.authHeaders.mobile)

    fun checkEmptySubmissionHealth() = getHealthContent(config.emptySubmissionEndpoint, config.authHeaders.mobile)

    fun checkAnalyticsEventSubmissionHealth() = getHealthContent(config.analyticsEventsSubmissionHealthEndpoint, config.authHeaders.mobile)

    private fun getHealthContent(uri: String, authHeader: String): Status = http(Request(Method.POST, uri)
        .header("Authorization", authHeader)).status
}