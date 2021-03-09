package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import smoke.env.SmokeTests

class UnauthorizedRequestsSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val unAuthHeaders = listOf(Pair("Content-Type", APPLICATION_JSON.value))
    private fun unAuthorizedPostRequest(uri: String) = Request(POST, uri).headers(unAuthHeaders)

    @Test
    fun `exposure notification circuit breaker`() {
        val uri = "${config.exposureNotificationCircuitBreakerEndpoint}/request"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `risky venue circuit breaker`() {
        val uri = "${config.riskyVenuesCircuitBreakerEndpoint}/request"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `order virology test`() {
        val uri = "${config.virologyKitEndpoint}/home-kit/order"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `retrieve virology test result`() {
        val uri = "${config.virologyKitEndpoint}/results"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload npex test result`() {
        val uri = config.testResultsNpexUploadEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload fiorano test result`() {
        val uri = config.testResultsFioranoUploadEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload english token-gen test result`() {
        val uri = config.engTokenGenUploadEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload welsh token-gen test result`() {
        val uri = config.wlsTokenGenUploadEndpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `diagnosis keys submission`() {
        val uri = config.diagnosisKeysSubmissionEndpoint
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
    fun `empty submission`() {
        val uri = config.emptySubmissionEndpoint
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
