package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig
import uk.nhs.nhsx.circuitbreakers.ResolutionResponse
import uk.nhs.nhsx.circuitbreakers.TokenResponse

class VenuesCircuitBreakerClient(private val client: JavaHttpClient,
                                 private val config: EnvConfig) {

    companion object {
        private val logger = LogManager.getLogger(VenuesCircuitBreakerClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.riskyVenuesCircuitBreakerEndpoint
    }

    fun requestCircuitBreaker(): TokenResponse {
        logger.info("requestCircuitBreaker")

        val uri = "${config.riskyVenuesCircuitBreakerEndpoint}/request"

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }

    fun resolutionCircuitBreaker(tokenResponse: TokenResponse): ResolutionResponse {
        logger.info("resolutionCircuitBreaker")

        val uri = "${config.riskyVenuesCircuitBreakerEndpoint}/resolution/${tokenResponse.approvalToken}"

        val request = Request(Method.GET, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }
}
