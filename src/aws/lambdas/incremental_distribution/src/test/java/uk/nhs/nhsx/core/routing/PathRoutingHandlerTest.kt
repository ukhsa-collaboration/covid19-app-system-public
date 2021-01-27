package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.routing.Routing.PathRoutingHandler
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.Companion.request
import java.util.*

class PathRoutingHandlerTest {

    @Test
    fun pathAndMethodMatches() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler = PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { responseEvent }

        assertThat(handler.handle(request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build()))
            .isSameAs(responseEvent)
    }

    @Test
    fun pathMatchesAndMethodDifferent() {
        val handler = PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { null }

        assertThat(handler.handle(request().withPath("/path/to/resource").withMethod(HttpMethod.GET).build()).statusCode)
            .isEqualTo(405)
    }

    @Test
    fun noMatchAtAll() {
        val handler = PathRoutingHandler("/path/to/resource", Optional.of(Routing.Method.POST)) { null }

        assertThat(handler.handle(request().withPath("/path/to/unknown/resource").withMethod(HttpMethod.GET).build()).statusCode)
            .isEqualTo(404)
    }

    @Test
    fun pathOnlyMatch() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler = PathRoutingHandler("/path/to/resource", Optional.empty()) { responseEvent }

        assertThat(handler.handle(request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build()))
            .isSameAs(responseEvent)

        assertThat(handler.handle(request().withPath("/path/to/resource").withMethod(HttpMethod.GET).build()))
            .isSameAs(responseEvent)
    }

    @Test
    fun customPathMatcher() {
        val responseEvent = APIGatewayProxyResponseEvent()
        val handler = PathRoutingHandler({ it.startsWith("/path") }, Optional.of(Routing.Method.POST)) { responseEvent }

        assertThat(handler.handle(request().withPath("/path").withMethod(HttpMethod.POST).build()))
            .isSameAs(responseEvent)

        assertThat(handler.handle(request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build()))
            .isSameAs(responseEvent)

        assertThat(handler.handle(request().withPath("/pat").withMethod(HttpMethod.POST).build()).statusCode)
            .isEqualTo(404)
    }
}