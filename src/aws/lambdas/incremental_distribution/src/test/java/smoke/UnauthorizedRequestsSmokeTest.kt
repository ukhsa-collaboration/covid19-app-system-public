package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import smoke.clients.DiagnosisKeysSubmissionClient
import smoke.clients.EnCircuitBreakerClient
import smoke.clients.VenuesCircuitBreakerClient
import smoke.clients.VirologyClient
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
        val uri = "${VirologyClient.baseUrlFrom(config)}/home-kit/order"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `retrieve virology test result`() {
        val uri = "${VirologyClient.baseUrlFrom(config)}/results"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload npex test result`() {
        val uri = VirologyClient.npexUploadEndpoint(config)
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload fiorano test result`() {
        val uri = VirologyClient.fioranoUploadEndpoint(config)
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload english token-gen test result`() {
        val uri = VirologyClient.engTokenGenUploadEndpoint(config)
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload welsh token-gen test result`() {
        val uri = VirologyClient.wlsTokenGenUploadEndpoint(config)
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

    private fun assertUnAuthorized(response: Response) {
        assertThat(response, hasStatus(Status.FORBIDDEN))
        assertThat(response, !hasHeader("x-amz-meta-signature"))
        assertThat(response, !hasHeader("x-amz-meta-signature-date"))
        assertThat(response, hasBody(""))
    }
}