package uk.nhs.nhsx.core.routing;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import static uk.nhs.nhsx.core.routing.Routing.Match.Matched;
import static uk.nhs.nhsx.core.routing.Routing.RouterMatch.matched;

public abstract class DelegatingRoutingHandler implements Routing.RoutingHandler {
    private final Routing.RoutingHandler delegate;

    protected DelegatingRoutingHandler(Routing.RoutingHandler delegate) {
        this.delegate = delegate;
    }

    public Routing.RoutingHandler getDelegate() {
        return delegate;
    }

    @Override
    public final Routing.RouterMatch match(APIGatewayProxyRequestEvent request) {
        Routing.RouterMatch match = delegate.match(request);
        return match.matchType == Matched ? matched(this) : match;
    }
}
