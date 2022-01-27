package smoke

import org.awaitility.Awaitility.await
import org.http4k.cloudnative.env.Environment
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import smoke.actors.createHandler
import smoke.clients.AwsLambda
import smoke.env.SmokeTests
import java.time.Duration

@Tag("serial")
class MaintenanceModeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

    private fun postRequest(uri: String, authHeader: String) =
        Request(POST, uri)
            .header("Authorization", authHeader)
            .header("Content-Type", APPLICATION_JSON.value)

    @Nested
    inner class VirologySubmissionLambda {
        @BeforeEach
        fun setup() = AwsLambda.enableMaintenanceMode(config.virology_submission_lambda_function_name)

        @AfterEach
        fun tearDown() = AwsLambda.disableMaintenanceMode(config.virology_submission_lambda_function_name)

        @Test
        fun `order virology test`() {
            assertInMaintenanceMode(
                postRequest(
                    "${config.virology_kit_endpoint}/home-kit/order",
                    config.auth_headers.mobile
                )
            )
        }

        @Test
        fun `retrieve virology test result`() {
            assertInMaintenanceMode(
                postRequest(
                    "${config.virology_kit_endpoint}/results",
                    config.auth_headers.mobile
                )
            )
        }
    }

    @Nested
    inner class VirologyUploadLambda {
        @BeforeEach
        fun setup() = AwsLambda.enableMaintenanceMode(config.virology_upload_lambda_function_name)

        @AfterEach
        fun tearDown() = AwsLambda.disableMaintenanceMode(config.virology_upload_lambda_function_name)

        @Test
        fun `upload npex test result`() {
            assertInMaintenanceMode(
                postRequest(
                    config.test_results_npex_upload_endpoint,
                    config.auth_headers.testResultUpload
                )
            )
        }

        @Test
        fun `upload fiorano test result`() {
            assertInMaintenanceMode(
                postRequest(
                    config.test_results_fiorano_upload_endpoint,
                    config.auth_headers.testResultUpload
                )
            )
        }

        @Test
        fun `upload english token-gen test result`() {
            assertInMaintenanceMode(
                postRequest(
                    config.test_results_eng_tokengen_upload_endpoint,
                    config.auth_headers.testResultUpload
                )
            )
        }

        @Test
        fun `upload welsh token-gen test result`() {
            assertInMaintenanceMode(
                postRequest(
                    config.test_results_wls_tokengen_upload_endpoint,
                    config.auth_headers.testResultUpload
                )
            )
        }
    }

    private fun assertInMaintenanceMode(request: Request) {
        await()
            .atMost(Duration.ofMinutes(1))
            .pollInterval(Duration.ofSeconds(1))
            .until {
                val response = client(request)
                response.status == SERVICE_UNAVAILABLE && response.bodyString() == ""
            }
    }
}
