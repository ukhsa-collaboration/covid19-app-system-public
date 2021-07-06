package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.withMethod
import java.util.*

class PathRoutingHandlerTest {

    @Test
    fun pathAndMethodMatches() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler =
            Routing.PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { _, _ -> responseEvent }

        assertThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(POST),
                TestContext()
            )
        )
            .isSameAs(responseEvent)
    }

    @Test
    fun pathMatchesAndMethodDifferent() {
        val handler =
            Routing.PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { _, _ -> TODO() }

        assertThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(GET),
                TestContext()
            ).statusCode
        )
            .isEqualTo(405)
    }

    @Test
    fun noMatchAtAll() {
        val handler =
            Routing.PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { _, _ -> TODO() }

        assertThat(
            handler(
                request().withPath("/path/to/unknown/resource")
                    .withMethod(GET),
                TestContext()
            ).statusCode
        )
            .isEqualTo(404)
    }

    @Test
    fun pathOnlyMatch() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler = Routing.PathRoutingHandler("/path/to/resource", Optional.empty()) { _, _ -> responseEvent }

        assertThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(POST),
                TestContext()
            )
        )
            .isSameAs(responseEvent)

        assertThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(GET),
                TestContext()
            )
        )
            .isSameAs(responseEvent)
    }

    @Test
    fun customPathMatcher() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler =
            Routing.PathRoutingHandler(
                { it.startsWith("/path") },
                Optional.of(Routing.Method.POST)
            ) { _, _ -> responseEvent }

        assertThat(
            handler(
                request().withPath("/path")
                    .withMethod(POST), TestContext()
            )
        )
            .isSameAs(responseEvent)

        assertThat(
            handler(
                request().withPath("/path/to/resource")
                    .withMethod(POST),
                TestContext()
            )
        )
            .isSameAs(responseEvent)

        assertThat(
            handler(
                request().withPath("/pat")
                    .withMethod(POST),
                TestContext()
            ).statusCode
        )
            .isEqualTo(404)
    }
}
