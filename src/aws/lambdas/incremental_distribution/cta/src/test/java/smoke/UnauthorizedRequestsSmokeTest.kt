package smoke

import org.http4k.cloudnative.env.Environment
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isNull
import uk.nhs.nhsx.testhelper.assertions.bodyString
import uk.nhs.nhsx.testhelper.assertions.hasStatus
import uk.nhs.nhsx.testhelper.assertions.signatureDateHeader
import uk.nhs.nhsx.testhelper.assertions.signatureHeader

class UnauthorizedRequestsSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)
    private val unAuthHeaders = listOf(Pair("Content-Type", APPLICATION_JSON.value))
    private fun unAuthorizedPostRequest(uri: String) = Request(POST, uri).headers(unAuthHeaders)

    @Test
    fun `exposure notification circuit breaker`() {
        val uri = "${config.exposure_notification_circuit_breaker_endpoint}/request"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `risky venue circuit breaker`() {
        val uri = "${config.risky_venues_circuit_breaker_endpoint}/request"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `order virology test`() {
        val uri = "${config.virology_kit_endpoint}/home-kit/order"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `retrieve virology test result`() {
        val uri = "${config.virology_kit_endpoint}/results"
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload npex test result`() {
        val uri = config.test_results_npex_upload_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload fiorano test result`() {
        val uri = config.test_results_fiorano_upload_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload english token-gen test result`() {
        val uri = config.test_results_eng_tokengen_upload_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload welsh token-gen test result`() {
        val uri = config.test_results_wls_tokengen_upload_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `diagnosis keys submission`() {
        val uri = config.diagnosis_keys_submission_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload risky postal districts`() {
        val uri = config.risky_post_districts_upload_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `upload risky venues`() {
        val uri = config.risky_venues_upload_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    @Test
    fun `submit analytics`() {
        val uri = config.analytics_submission_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        expectThat(response)
            .hasStatus(OK)  // analytics submissions are authorized further downstream
            .and {
                signatureHeader.isNull()
                signatureDateHeader.isNull()
                bodyString.isEmpty()
            }
    }

    @Test
    fun `empty submission`() {
        val uri = config.empty_submission_endpoint
        val request = unAuthorizedPostRequest(uri)
        val response = client(request)

        assertUnAuthorized(response)
    }

    private fun assertUnAuthorized(response: Response) {
        expectThat(response).hasStatus(FORBIDDEN).and {
            signatureHeader.isNull()
            signatureDateHeader.isNull()
            bodyString.isEmpty()
        }
    }
}
