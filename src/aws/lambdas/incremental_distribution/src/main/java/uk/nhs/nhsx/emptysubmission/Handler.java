package uk.nhs.nhsx.emptysubmission;

import uk.nhs.nhsx.core.*;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

public class Handler extends RoutingHandler {

    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(environment, awsAuthentication(ApiName.Mobile), StandardSigning.signResponseWithKeyGivenInSsm(clock, environment));
    }

    public Handler(Environment environment,
                   Authenticator authenticator,
                   ResponseSigner signer) {
        handler = withSignedResponses(
            environment,
            authenticator,
            signer,
            routes(
                path(Routing.Method.POST, "/submission/empty-submission", (r) ->
                    HttpResponses.ok()
                )
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
