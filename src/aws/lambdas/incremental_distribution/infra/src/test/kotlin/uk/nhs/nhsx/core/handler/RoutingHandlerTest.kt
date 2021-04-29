package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import java.util.Optional
import java.util.UUID

class RoutingHandlerTest {

    @Test
    fun noHeaders() {
        assertThrows(RuntimeException::class.java) {
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
        MyRoutingHandler("content-type").handleRequest(request, TestContext())
    }

    @Test
    fun uppercase() {
        MyRoutingHandler("Content-Type").handleRequest(request, TestContext())
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        MyRoutingHandler("content-type").handleRequest(request, TestContext().apply { requestId = uuid })

        assertThat(RequestContext.awsRequestId(), equalTo(uuid.toString()))
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
