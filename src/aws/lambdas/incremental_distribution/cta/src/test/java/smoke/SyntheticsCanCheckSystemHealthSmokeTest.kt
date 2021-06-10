package smoke

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import smoke.actors.Synthetics
import smoke.env.SmokeTests

class SyntheticsCanCheckSystemHealthSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val synthetics = Synthetics(client, config)

    @Test
    fun `analytics submission health`() {
        assertThat(synthetics.checkAnalyticsSubmissionHealth(), equalTo(OK))
    }

    @Test
    fun `diagnosis keys submission health`() {
        assertThat(synthetics.checkDiagnosisKeysSubmissionHealth(), equalTo(OK))
    }

    @Test
    fun `exposure notification circuit breaker health`() {
        assertThat(synthetics.checkEnCircuitBreakerHealth(), equalTo(OK))
    }

    @Test
    fun `isolation payment health`() {
        assertThat(synthetics.checkIsolationPaymentHealth(), equalTo(OK))
    }

    @Test
    fun `risky post districts upload health`() {
        assertThat(synthetics.checkRiskyPostDistrictsUploadHealth(), equalTo(OK))
    }

    @Test
    fun `risky venues circuit breaker health`() {
        assertThat(synthetics.checkRiskyVenuesCircuitBreakerHealth(), equalTo(OK))
    }

    @Test
    fun `risky venues upload health`() {
        assertThat(synthetics.checkRiskyVenuesUploadHealth(), equalTo(OK))
    }

    @Test
    fun `test results health`() {
        assertThat(synthetics.checkTestResultsUploadHealth(), equalTo(OK))
    }

    @Test
    fun `virology kit health`() {
        assertThat(synthetics.checkVirologyKitHealth(), equalTo(OK))
    }

    @Test
    fun `empty endpoint health`() {
        assertThat(synthetics.checkEmptySubmissionHealth(), equalTo(OK))
    }

    @Test
    fun `analytics event submission health`() {
        assertThat(synthetics.checkAnalyticsEventSubmissionHealth(), equalTo(OK))
    }

}