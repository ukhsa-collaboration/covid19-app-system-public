package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.After
import org.junit.Test
import smoke.clients.AwsLambda
import smoke.clients.VirologyClient
import smoke.env.SmokeTests

class MaintenanceModeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val headers = listOf(Pair("Content-Type", ContentType.APPLICATION_JSON.value))

    private fun postRequest(uri: String) =
        Request(Method.POST, uri).headers(headers)

    @After
    fun tearDown() {
        AwsLambda.disableMaintenanceMode(config.virologySubmissionLambdaFunctionName)
        AwsLambda.disableMaintenanceMode(config.virologyUploadLambdaFunctionName)
    }

    @Test
    fun `order virology test`() {
        AwsLambda.enableMaintenanceMode(config.virologySubmissionLambdaFunctionName)
        val uri = "${VirologyClient.baseUrlFrom(config)}/home-kit/order"
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `retrieve virology test result`() {
        AwsLambda.enableMaintenanceMode(config.virologySubmissionLambdaFunctionName)
        val uri = "${VirologyClient.baseUrlFrom(config)}/results"
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload npex test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = VirologyClient.npexUploadEndpoint(config)
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload fiorano test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = VirologyClient.fioranoUploadEndpoint(config)
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload english token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = VirologyClient.engTokenGenUploadEndpoint(config)
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    @Test
    fun `upload welsh token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        val uri = VirologyClient.wlsTokenGenUploadEndpoint(config)
        val request = postRequest(uri)
        val response = client(request)

        assertInMaintenanceMode(response)
    }

    private fun assertInMaintenanceMode(response: Response) {
        assertThat(response, hasStatus(Status.SERVICE_UNAVAILABLE))
        assertThat(response, hasBody(""))
    }
}