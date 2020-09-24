package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig
import uk.nhs.nhsx.TestData
import uk.nhs.nhsx.circuitbreakers.TokenResponse

class EnCircuitBreakerClient(private val client: JavaHttpClient,
                             private val config: EnvConfig) {


    companion object {
        private val logger = LogManager.getLogger(EnCircuitBreakerClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.exposureNotificationCircuitBreakerEndpoint
    }

    fun requestCircuitBreaker(): TokenResponse {
        logger.info("requestCircuitBreaker")

        val uri = "${config.exposureNotificationCircuitBreakerEndpoint}/request"

        val payload = TestData.EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(payload)

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }
}