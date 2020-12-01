package uk.nhs.nhsx.core.routing;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static uk.nhs.nhsx.ProxyRequestBuilder.request;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy;
import static uk.nhs.nhsx.core.routing.StandardHandlers.catchExceptions;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasStatus;

public class RoutingTest {

    @Test
    public void someExample() throws Exception {

        AtomicInteger chosen = new AtomicInteger();
        Authenticator authenticator = (r) -> true;

        Routing.Handler handler = catchExceptions(authorisedBy(authenticator, routes(
            path(POST, "/a", (r) ->  { chosen.set(1); return HttpResponses.ok(); }),
            path(POST, "/b", (r) ->  { chosen.set(2); return HttpResponses.ok(); })
        )));

        APIGatewayProxyResponseEvent response = handler.handle(
            request()
                .withMethod(HttpMethod.POST)
                .withBearerToken("something")
                .withPath("/a")
                .build()
        );

        MatcherAssert.assertThat(response, hasStatus(HttpStatusCode.OK_200));
        MatcherAssert.assertThat(chosen.get(), equalTo(1));
    }
}
