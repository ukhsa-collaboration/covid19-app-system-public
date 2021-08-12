package smoke

import org.http4k.cloudnative.env.Environment
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import smoke.actors.createHandler
import smoke.clients.AwsLambda
import smoke.env.SmokeTests
import uk.nhs.nhsx.testhelper.matchers.eventually

@Tag("serial")
class MaintenanceModeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

    private fun postRequest(uri: String, authHeader: String) =
        Request(POST, uri)
            .header("Authorization", authHeader)
            .header("Content-Type", APPLICATION_JSON.value)

    @AfterEach
    fun tearDown() {
        AwsLambda.disableMaintenanceMode(config.virology_submission_lambda_function_name)
        AwsLambda.disableMaintenanceMode(config.virology_upload_lambda_function_name)
    }

    @Test
    fun `order virology test`() {
        AwsLambda.enableMaintenanceMode(config.virology_submission_lambda_function_name)

        assertInMaintenanceMode(
            postRequest(
                "${config.virology_kit_endpoint}/home-kit/order",
                config.auth_headers.mobile
            )
        )
    }

    @Test
    fun `retrieve virology test result`() {
        AwsLambda.enableMaintenanceMode(config.virology_submission_lambda_function_name)

        assertInMaintenanceMode(
            postRequest(
                "${config.virology_kit_endpoint}/results",
                config.auth_headers.mobile
            )
        )
    }

    @Test
    fun `upload npex test result`() {
        AwsLambda.enableMaintenanceMode(config.virology_upload_lambda_function_name)

        assertInMaintenanceMode(
            postRequest(
                config.test_results_npex_upload_endpoint,
                config.auth_headers.testResultUpload
            )
        )
    }

    @Test
    fun `upload fiorano test result`() {
        AwsLambda.enableMaintenanceMode(config.virology_upload_lambda_function_name)

        assertInMaintenanceMode(
            postRequest(
                config.test_results_fiorano_upload_endpoint,
                config.auth_headers.testResultUpload
            )
        )
    }

    @Test
    fun `upload english token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virology_upload_lambda_function_name)
        assertInMaintenanceMode(
            postRequest(
                config.test_results_eng_tokengen_upload_endpoint,
                config.auth_headers.testResultUpload
            )
        )
    }

    @Test
    fun `upload welsh token-gen test result`() {
        AwsLambda.enableMaintenanceMode(config.virology_upload_lambda_function_name)

        assertInMaintenanceMode(
            postRequest(
                config.test_results_wls_tokengen_upload_endpoint,
                config.auth_headers.testResultUpload
            )
        )
    }

    private fun assertInMaintenanceMode(request: Request) {
        val matcher: (Response) -> Boolean = { r ->
            r.status == SERVICE_UNAVAILABLE && r.bodyString() == ""
        }

        eventually(matcher) { client(request) }
    }
}
