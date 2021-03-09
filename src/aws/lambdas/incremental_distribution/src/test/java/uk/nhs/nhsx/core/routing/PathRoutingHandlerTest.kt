package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.routing.Routing.PathRoutingHandler
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.Companion.request
import java.util.Optional

class PathRoutingHandlerTest {

    @Test
    fun pathAndMethodMatches() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler =
            PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { _, _ -> responseEvent }

        assertThat(
            handler(
                request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build(),
                TestContext()
            )
        )
            .isSameAs(responseEvent)
    }

    @Test
    fun pathMatchesAndMethodDifferent() {
        val handler = PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { _, _ -> TODO() }

        assertThat(
            handler(
                request().withPath("/path/to/resource").withMethod(HttpMethod.GET).build(),
                TestContext()
            ).statusCode
        )
            .isEqualTo(405)
    }

    @Test
    fun noMatchAtAll() {
        val handler = PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { _, _ -> TODO() }

        assertThat(
            handler(
                request().withPath("/path/to/unknown/resource").withMethod(HttpMethod.GET).build(),
                TestContext()
            ).statusCode
        )
            .isEqualTo(404)
    }

    @Test
    fun pathOnlyMatch() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler = PathRoutingHandler("/path/to/resource", Optional.empty()) { _, _ -> responseEvent }

        assertThat(
            handler(
                request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build(),
                TestContext()
            )
        )
            .isSameAs(responseEvent)

        assertThat(
            handler(
                request().withPath("/path/to/resource").withMethod(HttpMethod.GET).build(),
                TestContext()
            )
        )
            .isSameAs(responseEvent)
    }

    @Test
    fun customPathMatcher() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler =
            PathRoutingHandler({ it.startsWith("/path") }, Optional.of(Routing.Method.POST)) { _, _ -> responseEvent }

        assertThat(handler(request().withPath("/path").withMethod(HttpMethod.POST).build(), TestContext()))
            .isSameAs(responseEvent)

        assertThat(
            handler(
                request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build(),
                TestContext()
            )
        )
            .isSameAs(responseEvent)

        assertThat(
            handler(
                request().withPath("/pat").withMethod(HttpMethod.POST).build(),
                TestContext()
            ).statusCode
        )
            .isEqualTo(404)
    }
}
