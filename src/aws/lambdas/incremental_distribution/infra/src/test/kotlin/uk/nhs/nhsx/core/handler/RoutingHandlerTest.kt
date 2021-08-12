package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import java.util.*

class RoutingHandlerTest {

    @Test
    fun `no headers`() {
        expectThrows<RuntimeException> {
            MyRoutingHandler("content-type").handleRequest(
                APIGatewayProxyRequestEvent(),
                TestContext()
            )
        }
    }

    private val request = APIGatewayProxyRequestEvent().apply {
        headers = mapOf("content-type" to "something")
    }

    @Test
    fun lowercase() {
        expectCatching {
            MyRoutingHandler("content-type").handleRequest(request, TestContext())
        }.isSuccess()
    }

    @Test
    fun uppercase() {
        expectCatching {
            MyRoutingHandler("Content-Type").handleRequest(request, TestContext())
        }.isSuccess()
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        MyRoutingHandler("content-type").handleRequest(request, TestContext().apply { requestId = uuid })

        expectThat(RequestContext.awsRequestId()).isEqualTo(uuid.toString())
    }

    private class MyRoutingHandler(val header: String) : RoutingHandler() {
        override fun handler(): ApiGatewayHandler =
            ApiGatewayHandler { request, _ ->
                Optional.ofNullable(request.headers[header])
                    .map { HttpResponses.ok() }
                    .orElseThrow { RuntimeException("not there") }
            }
    }
}
