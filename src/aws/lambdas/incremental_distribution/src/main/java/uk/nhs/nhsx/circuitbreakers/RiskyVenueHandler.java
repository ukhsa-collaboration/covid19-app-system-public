package uk.nhs.nhsx.circuitbreakers;

import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;
import uk.nhs.nhsx.core.aws.ssm.Parameters;
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

    private static final ParameterName initial = ParameterName.of("venue-notification-initial");
    private static final ParameterName poll = ParameterName.of("venue-notification-poll");

    private final Routing.Handler handler;


    public RiskyVenueHandler() {
        this(SystemClock.CLOCK, Environment.fromSystem());
    }

    public RiskyVenueHandler(Supplier<Instant> clock, Environment environment) {
        this(
            environment,
            awsAuthentication(ApiName.Mobile),
            signResponseWithKeyGivenInSsm(clock, environment),
            new AwsSsmParameters()
        );
    }

    public RiskyVenueHandler(Environment environment, Authenticator authenticator, ResponseSigner signer, Parameters parameters) {
        this(authenticator, signer,  new CircuitBreakerService(
            parameters.ofEnum(initial.withPrefix(environment.access.required("SSM_CIRCUIT_BREAKER_BASE_NAME")), ApprovalStatus.class, ApprovalStatus.PENDING ),
            parameters.ofEnum(poll.withPrefix(environment.access.required("SSM_CIRCUIT_BREAKER_BASE_NAME")), ApprovalStatus.class, ApprovalStatus.PENDING )
        ));
    }

    public RiskyVenueHandler(Authenticator authenticator, ResponseSigner signer, CircuitBreakerService circuitBreakerService) {
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
