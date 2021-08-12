package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.routing.Routing.Method
import uk.nhs.nhsx.core.routing.Routing.PathRoutingHandler
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.withMethod
import java.util.*

class PathRoutingHandlerTest {

    @Test
    fun `path and method matches`() {
        val responseEvent = APIGatewayProxyResponseEvent()

        val handler = PathRoutingHandler(
            "/path/to/resource",
            Optional.of(Method.POST)
        ) { _, _ -> responseEvent }

        expectThat(handler(request().withPath("/path/to/resource").withMethod(POST), TestContext()))
            .isEqualTo(responseEvent)
    }

    @Test
    fun pathMatchesAndMethodDifferent() {
        val handler = PathRoutingHandler(
            "/path/to/resource",
            Optional.of(Method.POST)
        ) { _, _ -> TODO() }

        expectThat(
            handler(
                request().withPath("/path/to/resource").withMethod(GET),
                TestContext()
            )
        ).status.isSameAs(METHOD_NOT_ALLOWED)
    }

    @Test
    fun noMatchAtAll() {
        val handler = PathRoutingHandler(
            "/path/to/resource",
            Optional.of(Method.POST)
        ) { _, _ -> TODO() }

        expectThat(
            handler(
                request().withPath("/path/to/unknown/resource")
                    .withMethod(GET),
                TestContext()
            )
        ).status.isSameAs(NOT_FOUND)
    }

    @Test
    fun pathOnlyMatch() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler = PathRoutingHandler(
            "/path/to/resource",
            Optional.empty()
        ) { _, _ -> responseEvent }

        expectThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(POST),
                TestContext()
            )
        ).isEqualTo(responseEvent)

        expectThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(GET),
                TestContext()
            )
        ).isEqualTo(responseEvent)
    }

    @Test
    fun customPathMatcher() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler = PathRoutingHandler(
            { it.startsWith("/path") },
            Optional.of(Method.POST)
        ) { _, _ -> responseEvent }

        expectThat(
            handler(
                request().withPath("/path")
                    .withMethod(POST), TestContext()
            )
        ).isEqualTo(responseEvent)

        expectThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(POST),
                TestContext()
            )
        ).isEqualTo(responseEvent)

        expectThat(
            handler(
                request().withPath("/pat")
                    .withMethod(POST),
                TestContext()
            )
        ).status.isSameAs(NOT_FOUND)
    }
}
