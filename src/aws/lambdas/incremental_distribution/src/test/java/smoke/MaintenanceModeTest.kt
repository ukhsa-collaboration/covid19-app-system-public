package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import smoke.clients.AwsLambda
import smoke.env.SmokeTests

class MaintenanceModeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val headers = listOf(Pair("Content-Type", ContentType.APPLICATION_JSON.value))

    private fun postRequest(uri: String) =
        Request(Method.POST, uri).headers(headers)

    @AfterEach
    fun tearDown() {
        AwsLambda.disableMaintenanceMode(config.virologySubmissionLambdaFunctionName)
        AwsLambda.disableMaintenanceMode(config.virologyUploadLambdaFunctionName)
    }

    @Test
    fun `order virology test`() {
        AwsLambda.enableMaintenanceMode(config.virologySubmissionLambdaFunctionName)
        val uri = "${config.virologyKitEndpoint}/home-kit/order"
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `retrieve virology test result`() {
        AwsLambda.enableMaintenanceMode(config.virologySubmissionLambdaFunctionName)
        val uri = "${config.virologyKitEndpoint}/results"
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload npex test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = config.testResultsNpexUploadEndpoint
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload fiorano test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = config.testResultsFioranoUploadEndpoint
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload english token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = config.engTokenGenUploadEndpoint
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload welsh token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = config.wlsTokenGenUploadEndpoint
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    private fun assertInMaintenanceMode(response: Response) {
        assertThat(response, hasStatus(Status.SERVICE_UNAVAILABLE))
        assertThat(response, hasBody(""))
    }
}