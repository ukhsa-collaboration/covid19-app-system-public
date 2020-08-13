package uk.nhs.nhsx.circuitbreakers;

import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.startsWith;
import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;


/**
 * Lambda Facade for Risky Venue Circuit Breakers
 * <p>
 * - doc/design/api-contracts/risky-venue-circuit-breaker.md
 * <p>
 * Hints:
 * - ".../request" always returns "pending" and a random token (no persistence)
 * - ".../resolution/<approval_token>" always returns "yes"
 */

public class RiskyVenueHandler extends RoutingHandler {

    private final Routing.Handler handler;

    public RiskyVenueHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public RiskyVenueHandler(Environment environment, Supplier<Instant> clock) {
        this(awsAuthentication(ApiName.Mobile), signResponseWithKeyGivenInSsm(clock, environment));
    }

    public RiskyVenueHandler(Authenticator authenticator, ResponseSigner signer) {
        CircuitBreakerService circuitBreakerService = new CircuitBreakerService();
        this.handler = withSignedResponses(
            authenticator,
            signer, routes(
                path(Routing.Method.POST, startsWith("/circuit-breaker/venue/request"),
                    r -> deserializeMaybe(r.getBody(), RiskyVenueCircuitBreakerRequest.class)
                        .map(it -> circuitBreakerService.getApprovalToken())
                        .orElseThrow(() -> new ApiResponseException(UNPROCESSABLE_ENTITY_422))),
                path(Routing.Method.GET, startsWith("/circuit-breaker/venue/resolution"),
                    r -> circuitBreakerService.getResolution(r.getPath()))
            )
        );
    }


    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
