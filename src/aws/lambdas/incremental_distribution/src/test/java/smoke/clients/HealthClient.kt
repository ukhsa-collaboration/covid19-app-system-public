package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import smoke.env.EnvConfig

class HealthClient(private val client: JavaHttpClient,
                   private val config: EnvConfig) {

    private val logger = LogManager.getLogger(HealthClient::class.java)

    fun analyticsSubmission() = getHealthContent(config.analyticsSubmissionHealthEndpoint, config.authHeaders.mobile)

    fun diagnosisKeysSubmission() = getHealthContent(config.diagnosisKeysSubmissionHealthEndpoint, config.authHeaders.mobile)

    fun enCircuitBreakerHealthEndpoint() = getHealthContent(config.enCircuitBreakerHealthEndpoint, config.authHeaders.mobile)

    fun isolationPaymentHealthEndpoint() = getHealthContent(config.isolationPaymentHealthEndpoint, config.authHeaders.mobile)

    fun riskyPostDistrictsUploadHealthEndpoint() = getHealthContent(config.riskyPostDistrictsUploadHealthEndpoint, config.authHeaders.highRiskPostCodeUpload)

    fun riskyVenuesCircuitBreakerHealthEndpoint() = getHealthContent(config.riskyVenuesCircuitBreakerHealthEndpoint, config.authHeaders.mobile)

    fun riskyVenuesUploadHealthEndpoint() = getHealthContent(config.riskyVenuesUploadHealthEndpoint, config.authHeaders.highRiskVenuesCodeUpload)

    fun testResultsHealthEndpoint() = getHealthContent(config.testResultsHealthEndpoint, config.authHeaders.testResultUpload)

    fun virologyKitHealthEndpoint() = getHealthContent(config.virologyKitHealthEndpoint, config.authHeaders.mobile)

    fun emptySubmissionEndpoint() = getHealthContent(config.emptySubmissionEndpoint, config.authHeaders.mobile)

    private fun getHealthContent(uri: String, authHeader: String): Response {
        logger.info("getHealthContent: $uri")

        val request = Request(Method.POST, uri)
            .header("Authorization", authHeader)

        return client(request)
    }


}