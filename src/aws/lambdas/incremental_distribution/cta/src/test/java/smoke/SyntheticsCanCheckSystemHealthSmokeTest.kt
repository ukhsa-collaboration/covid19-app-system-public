package smoke

import org.http4k.cloudnative.env.Environment
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import smoke.actors.Synthetics
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SyntheticsCanCheckSystemHealthSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

    private val synthetics = Synthetics(client, config)

    @Test
    fun `analytics submission health`() {
        expectThat(synthetics.checkAnalyticsSubmissionHealth()).isEqualTo(OK)
    }

    @Test
    fun `diagnosis keys submission health`() {
        expectThat(synthetics.checkDiagnosisKeysSubmissionHealth()).isEqualTo(OK)
    }

    @Test
    fun `exposure notification circuit breaker health`() {
        expectThat(synthetics.checkEnCircuitBreakerHealth()).isEqualTo(OK)
    }

    @Test
    fun `isolation payment health`() {
        expectThat(synthetics.checkIsolationPaymentHealth()).isEqualTo(OK)
    }

    @Test
    fun `risky post districts upload health`() {
        expectThat(synthetics.checkRiskyPostDistrictsUploadHealth()).isEqualTo(OK)
    }

    @Test
    fun `risky venues circuit breaker health`() {
        expectThat(synthetics.checkRiskyVenuesCircuitBreakerHealth()).isEqualTo(OK)
    }

    @Test
    fun `risky venues upload health`() {
        expectThat(synthetics.checkRiskyVenuesUploadHealth()).isEqualTo(OK)
    }

    @Test
    fun `test results health`() {
        expectThat(synthetics.checkTestResultsUploadHealth()).isEqualTo(OK)
    }

    @Test
    fun `virology kit health`() {
        expectThat(synthetics.checkVirologyKitHealth()).isEqualTo(OK)
    }

    @Test
    fun `empty endpoint health`() {
        expectThat(synthetics.checkEmptySubmissionHealth()).isEqualTo(OK)
    }

    @Test
    fun `analytics event submission health`() {
        expectThat(synthetics.checkAnalyticsEventSubmissionHealth()).isEqualTo(OK)
    }

}
