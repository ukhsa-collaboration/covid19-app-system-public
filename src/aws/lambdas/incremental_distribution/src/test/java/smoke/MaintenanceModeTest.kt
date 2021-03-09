package smoke

import com.natpryce.hamkrest.and
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.filter.debug
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import smoke.clients.AwsLambda
import smoke.env.SmokeTests
import uk.nhs.nhsx.testhelper.matchers.eventually

@Tag("serial")
class MaintenanceModeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient().debug()

    private fun postRequest(uri: String, authHeader: String) =
        Request(POST, uri)
            .header("Authorization", authHeader)
            .header("Content-Type", APPLICATION_JSON.value)

    @AfterEach
    fun tearDown() {
        AwsLambda.disableMaintenanceMode(config.virologySubmissionLambdaFunctionName)
        AwsLambda.disableMaintenanceMode(config.virologyUploadLambdaFunctionName)
    }

    @Test
    fun `order virology test`() {
        AwsLambda.enableMaintenanceMode(config.virologySubmissionLambdaFunctionName)

        assertInMaintenanceMode(
            postRequest(
                "${config.virologyKitEndpoint}/home-kit/order",
                config.authHeaders.mobile
            )
        )
    }

    @Test
    fun `retrieve virology test result`() {
        AwsLambda.enableMaintenanceMode(config.virologySubmissionLambdaFunctionName)

        assertInMaintenanceMode(
            postRequest(
                "${config.virologyKitEndpoint}/results",
                config.authHeaders.mobile
            )
        )
    }

    @Test
    fun `upload npex test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)

        assertInMaintenanceMode(postRequest(config.testResultsNpexUploadEndpoint, config.authHeaders.testResultUpload))
    }

    @Test
    fun `upload fiorano test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)

        assertInMaintenanceMode(
            postRequest(
                config.testResultsFioranoUploadEndpoint,
                config.authHeaders.testResultUpload
            )
        )
    }

    @Test
    fun `upload english token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)
        assertInMaintenanceMode(postRequest(config.engTokenGenUploadEndpoint, config.authHeaders.testResultUpload))
    }

    @Test
    fun `upload welsh token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virologyUploadLambdaFunctionName)

        assertInMaintenanceMode(postRequest(config.wlsTokenGenUploadEndpoint, config.authHeaders.testResultUpload))
    }

    private fun assertInMaintenanceMode(request: Request) {
        eventually(hasStatus(SERVICE_UNAVAILABLE).and(hasBody(""))) {
            client(request)
        }
    }
}
