package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig
import uk.nhs.nhsx.circuitbreakers.RiskyVenueCircuitBreakerRequest
import uk.nhs.nhsx.circuitbreakers.TokenResponse
import uk.nhs.nhsx.core.Jackson

class VenuesCircuitBreakerClient(private val client: JavaHttpClient,
                                 private val config: EnvConfig) {

    companion object {
        private val logger = LogManager.getLogger(VenuesCircuitBreakerClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.riskyVenuesCircuitBreakerEndpoint
    }

    fun requestCircuitBreaker(circuitBreakerRequest: RiskyVenueCircuitBreakerRequest): TokenResponse {
        logger.info("requestCircuitBreaker")

        val uri = "${config.riskyVenuesCircuitBreakerEndpoint}/request"

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Jackson.toJson(circuitBreakerRequest))

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }
}
