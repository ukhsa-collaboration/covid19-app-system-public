package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import smoke.clients.*
import smoke.env.SmokeTests

class UnauthorizedRequestsSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val unAuthHeaders = listOf(Pair("Content-Type", ContentType.APPLICATION_JSON.value))
    private fun unAuthorizedPostRequest(uri: String) =
        Request(Method.POST, uri).headers(unAuthHeaders)

    @Test
    fun `exposure notification circuit breaker`() {
        val uri = "${EnCircuitBreakerClient.baseUrlFrom(config)}/request"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `risky venue circuit breaker`() {
        val uri = "${VenuesCircuitBreakerClient.baseUrlFrom(config)}/request"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `order virology test`() {
        val uri = "${TestKitOrderClient.baseUrlFrom(config)}/home-kit/order"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `retrieve virology test result`() {
        val uri = "${TestKitOrderClient.baseUrlFrom(config)}/results"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload test result`() {
        val uri = TestResultUploadClient.baseUrlFrom(config)
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `diagnosis keys submission`() {
        val uri = DiagnosisKeysSubmissionClient.baseUrlFrom(config)
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload risky postal districts`() {
        val uri = config.riskyPostDistrictsUploadEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload risky venues`() {
        val uri = config.riskyVenuesUploadEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `submit analytics`() {
        val uri = config.analyticsSubmissionEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `submit activation codes`() {
        val uri = config.activationKeysSubmissionEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)

    }

    private fun assertUnAuthorized(response: Response) {
        assertThat(response, hasStatus(Status.FORBIDDEN))
        assertThat(response, !hasHeader("x-amz-meta-signature"))
        assertThat(response, !hasHeader("x-amz-meta-signature-date"))
        assertThat(response, hasBody(""))
    }
}