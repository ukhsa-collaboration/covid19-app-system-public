package uk.nhs.nhsx.sanity.common

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.format.Jackson.auto
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.sanity.LambdaSanityCheck
import uk.nhs.nhsx.sanity.config.CircuitBreaker

class CircuitBreakerSanityChecks : LambdaSanityCheck() {

    @MethodSource("circuitBreakers")
    @ParameterizedTest(name = "CircuitBreaker request returns a 403 {arguments}")
    fun `CircuitBreaker request returns a 403`(circuitBreaker: CircuitBreaker) {
        assertThat(
            insecureClient(Request(POST, circuitBreaker.endpointUri.extend(Uri.of("/request")))),
            hasStatus(FORBIDDEN)
        )
    }

    @Test
    fun `Exposure Notification CircuitBreaker resolution returns a 200`() {
        circuitBreakers()
            .first { it.name == "exposure_notification_circuit_breaker" }
            .apply { assertFlow("""{ "matchedKeyCount": 2, "daysSinceLastExposure": 3, "maximumRiskScore": 150.0, "riskCalculationVersion": 2 }""") }
    }

    @Test
    fun `Risky Venue CircuitBreaker resolution returns a 200`() {
        circuitBreakers()
            .first { it.name == "risky_venues_circuit_breaker" }
            .apply { assertFlow() }
    }

    private fun CircuitBreaker.assertFlow(bodyString: String? = null) {
        val initialRequest = Request(POST, endpointUri.extend(Uri.of("/request"))).body(bodyString.orEmpty())
        val initialResponse = withSecureClient(initialRequest)
        assertThat(initialResponse, hasStatus(OK))

        val approval = Body.auto<Map<String, String>>()
            .toLens()
            .extract(initialResponse)
            .getValue("approval")

        assertThat(approval, equalTo("yes"))

        val approvalToken = Body.auto<Map<String, String>>()
            .toLens()
            .extract(initialResponse)
            .getValue("approvalToken")

        val pollingRequest = Request(GET, endpointUri.extend(Uri.of("/resolution/$approvalToken")))
        val pollingResponse = withSecureClient(pollingRequest)
        assertThat(pollingResponse, hasStatus(OK))
    }

    companion object {
        @JvmStatic
        private fun circuitBreakers() = endpoints().filterIsInstance<CircuitBreaker>()
    }
}
