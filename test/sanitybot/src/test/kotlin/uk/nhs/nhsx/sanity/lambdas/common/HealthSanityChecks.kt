package uk.nhs.nhsx.sanity.lambdas.common

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.sanity.lambdas.LambdaSanityCheck
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.RiskyPostcodeDistrictsUpload
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.RiskyVenuesUpload
import uk.nhs.nhsx.sanity.lambdas.config.HealthCheck

class HealthSanityChecks : LambdaSanityCheck() {

//    Check analytics submission health endpoint - POST 200 ✅
//    Check analytics events submission health endpoint - POST 200 ✅
//    Check diagnosis keys submission health endpoint - POST 200 ✅
//    Check exposure notification circuit breaker health endpoint - POST 200 ✅
//    Check isolation payment health endpoint - POST 200 ✅
//    Check risky venues circuit breaker health endpoint - POST 200 ✅
//    Check virology kit health endpoint - POST 200 ✅
//    Check virology test upload health endpoint - POST 200 ✅ <--- virology is one lambda serving multiple endpoints

    @MethodSource("lambdasWithHealth")
    @ParameterizedTest(name = "Health endpoint returns a 200 {arguments}")
    fun `Health endpoint returns a 200`(healthCheck: HealthCheck) {
        assertThat(healthCheck.withSecureClient(Request(POST, healthCheck.healthEndpoint)), hasStatus(OK))
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun lambdasWithHealth() = healthEndPoints()
            .filterNot { it.name == RiskyVenuesUpload.name }
            .filterNot { it.name == RiskyPostcodeDistrictsUpload.name }
            .filterIsInstance<HealthCheck>()

    }
}
