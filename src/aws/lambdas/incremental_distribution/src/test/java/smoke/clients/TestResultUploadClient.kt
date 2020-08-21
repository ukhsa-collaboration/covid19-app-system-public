package smoke.clients

import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.slf4j.LoggerFactory
import smoke.env.EnvConfig
import uk.nhs.nhsx.testkitorder.order.TestOrderResponse

class TestResultUploadClient(private val client: JavaHttpClient,
                             private val config: EnvConfig) {

    companion object {
        private val logger = LoggerFactory.getLogger(TestResultUploadClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.testResultsUploadEndpoint
    }

    fun uploadTestResult(testOrderResponse: TestOrderResponse) {
        logger.info("uploadTestResult")

        val uri = baseUrlFrom(config)

        val payload = """
            {
              "ctaToken": "${testOrderResponse.tokenParameterValue}",
              "testEndDate": "2020-04-23T00:00:00Z",
              "testResult": "POSITIVE"
            }
        """

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.testResultUpload)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(payload)

        val response = client(request)

        response
            .requireStatusCode(Status.ACCEPTED)
            .requireBodyText("successfully processed")
    }
}