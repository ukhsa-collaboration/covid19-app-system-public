package uk.nhs.nhsx.core.routing;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.Test;
import uk.nhs.nhsx.ProxyRequestBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PathRoutingHandlerTest {

    @Test
    public void pathAndMethodMatches() {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        Routing.PathRoutingHandler handler = new Routing.PathRoutingHandler(
            "/path/to/resource",
            Optional.of(Routing.Method.POST),
            (request) -> responseEvent
        );

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build()
        )).isSameAs(responseEvent);
    }

    @Test
    public void pathMatchesAndMethodDifferent() {
        Routing.PathRoutingHandler handler = new Routing.PathRoutingHandler(
            "/path/to/resource",
            Optional.of(Routing.Method.POST),
            (request) -> null
        );

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/path/to/resource").withMethod(HttpMethod.GET).build()
        ).getStatusCode()).isEqualTo(405);
    }

    @Test
    public void noMatchAtAll() {
        Routing.PathRoutingHandler handler = new Routing.PathRoutingHandler(
            "/path/to/resource",
            Optional.of(Routing.Method.POST),
            (request) -> null
        );

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/path/to/unknown/resource").withMethod(HttpMethod.GET).build()
        ).getStatusCode()).isEqualTo(404);
    }

    @Test
    public void pathOnlyMatch() {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        Routing.PathRoutingHandler handler = new Routing.PathRoutingHandler(
            "/path/to/resource",
            Optional.empty(),
            (request) -> responseEvent
        );

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build()
        )).isSameAs(responseEvent);

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/path/to/resource").withMethod(HttpMethod.GET).build()
        )).isSameAs(responseEvent);
    }

    @Test
    public void customPathMatcher() {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        Routing.PathRoutingHandler handler = new Routing.PathRoutingHandler(
                (path) -> path.startsWith("/path"),
            Optional.of(Routing.Method.POST),
            (request) -> responseEvent
        );

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/path").withMethod(HttpMethod.POST).build()
        )).isSameAs(responseEvent);

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/path/to/resource").withMethod(HttpMethod.POST).build()
        )).isSameAs(responseEvent);

        assertThat(handler.handle(
            ProxyRequestBuilder.request().withPath("/pat").withMethod(HttpMethod.POST).build()
        ).getStatusCode()).isEqualTo(404);

    }

}
