package smoke.clients

import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.slf4j.LoggerFactory
import smoke.env.EnvConfig
import uk.nhs.nhsx.testkitorder.lookup.TestLookupResponse
import uk.nhs.nhsx.testkitorder.order.TestOrderResponse

class TestKitOrderClient(private val client: JavaHttpClient,
                         private val config: EnvConfig) {


    companion object {
        private val logger = LoggerFactory.getLogger(TestKitOrderClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.virologyKitEndpoint
    }

    fun orderTest(): TestOrderResponse {
        logger.info("orderTest")

        val uri = "${baseUrlFrom(config)}/home-kit/order"

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }

    fun retrieveTestResult(testOrderResponse: TestOrderResponse): TestLookupResponse {
        logger.info("retrieveTestResult")

        val uri = "${baseUrlFrom(config)}/results"

        val payload = """
            {
              "testResultPollingToken": "${testOrderResponse.testResultPollingToken}"
            }
        """

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(payload)

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }
}