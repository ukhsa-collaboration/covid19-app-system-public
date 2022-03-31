package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import java.util.*

class RoutingHandlerTest {

    private val request = APIGatewayProxyRequestEvent()
        .apply { headers = mapOf("content-type" to "something") }

    @Test
    fun `is empty when no headers are present`() {
        val handler = object : RoutingHandler() {
            override fun handler() = ApiGatewayHandler { r, _ ->
                expectThat(r.headers).isEmpty()
                HttpResponses.ok()
            }
        }

        handler.handleRequest(APIGatewayProxyRequestEvent(), TestContext())
    }

    @Test
    fun `is null safe`() {
        val handler = object : RoutingHandler() {
            override fun handler() = ApiGatewayHandler { r, _ ->
                expectThat(r.headers).isEmpty()
                HttpResponses.ok()
            }
        }

        handler.handleRequest(APIGatewayProxyRequestEvent().apply { headers = null }, TestContext())
    }

    @ParameterizedTest
    @ValueSource(strings = ["Content-Type", "content-type", "CONTENT-TYPE"])
    fun `is case-insensitive`(header: String) {
        expectCatching { MyRoutingHandler(header).handleRequest(request, TestContext()) }
            .isSuccess()
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        val testContext = TestContext().apply { requestId = uuid }

        MyRoutingHandler("content-type")
            .handleRequest(request, testContext)

        expectThat(RequestContext.awsRequestId()).isEqualTo(uuid.toString())
    }

    private class MyRoutingHandler(val expectedHeaderName: String) : RoutingHandler() {
        override fun handler() = ApiGatewayHandler { r, _ ->
            expectThat(r.headers).isA<TreeMap<String, String>>()
            expectThat(r.headers).containsKey(expectedHeaderName)
            HttpResponses.ok()
        }
    }
}
