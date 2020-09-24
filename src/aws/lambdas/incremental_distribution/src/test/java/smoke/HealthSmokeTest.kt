package smoke

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.Test
import smoke.clients.HealthClient
import smoke.clients.requireStatusCode
import smoke.env.SmokeTests

class HealthSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val healthClient = HealthClient(client, config)

    @Test
    fun `activation keys submission health`() {
        verifyResponse(healthClient.activationKeysSubmission())
    }

    @Test
    fun `analytics submission health`() {
        verifyResponse(healthClient.analyticsSubmission())
    }

    @Test
    fun `diagnosis keys submission health`() {
        verifyResponse(healthClient.diagnosisKeysSubmission())
    }

    @Test
    fun `exposure notification circuit breaker health`() {
        verifyResponse(healthClient.enCircuitBreakerHealthEndpoint())
    }

    @Test
    fun `risky post districts upload health`() {
        verifyResponse(healthClient.riskyPostDistrictsUploadHealthEndpoint())
    }

    @Test
    fun `risky venues circuit breaker health`() {
        verifyResponse(healthClient.riskyVenuesCircuitBreakerHealthEndpoint())
    }

    @Test
    fun `risky venues upload health`() {
        verifyResponse(healthClient.riskyVenuesUploadHealthEndpoint())
    }

    @Test
    fun `test results health`() {
        verifyResponse(healthClient.testResultsHealthEndpoint())
    }

    @Test
    fun `virology kit health`() {
        verifyResponse(healthClient.virologyKitHealthEndpoint())
    }

    private fun verifyResponse(response: Response) {
        response.requireStatusCode(Status.OK)
            .bodyString()
    }
}