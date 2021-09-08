package uk.nhs.nhsx.sanity.common

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.sanity.LambdaSanityCheck
import uk.nhs.nhsx.sanity.config.CircuitBreaker

class CircuitBreakerSanityChecks : LambdaSanityCheck() {

    //Check risky venue circuit breaker - POST gets 403 + GET resolution/FAKE gets 200 ✅
    //Check exposure notification circuit breaker - POST gets 403 + GET resolution/FAKE gets 200 ✅

    @MethodSource("circuitBreaker")
    @ParameterizedTest(name = "CircuitBreaker request returns a 403 {arguments}")
    fun `CircuitBreaker request returns a 403`(circuitBreaker: CircuitBreaker) {
        assertThat(insecureClient(Request(POST, circuitBreaker.endpointUri.extend(Uri.of("/request")))),
            hasStatus(FORBIDDEN))
    }

    @MethodSource("circuitBreaker")
    @ParameterizedTest(name = "CircuitBreaker resolution returns a 200 {arguments}")
    fun `CircuitBreaker resolution returns a 200`(circuitBreaker: CircuitBreaker) {
        assertThat(circuitBreaker.withSecureClient(Request(GET, circuitBreaker.endpointUri.extend(Uri.of("/resolution/FAKE")))),
            hasStatus(OK))
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun circuitBreaker(): List<Any> = endpoints().filterIsInstance<CircuitBreaker>()
    }
}
