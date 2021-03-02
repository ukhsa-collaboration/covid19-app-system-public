package uk.nhs.nhsx.circuitbreakers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.EnvironmentKeys;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;
import uk.nhs.nhsx.core.aws.ssm.Parameters;
import uk.nhs.nhsx.core.events.CircuitBreakerVenueRequest;
import uk.nhs.nhsx.core.events.CircuitBreakerVenueResolution;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.startsWith;
import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.GET;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

public class RiskyVenueHandler extends RoutingHandler {

    private static final ParameterName initial = ParameterName.of("venue-notification-initial");
    private static final ParameterName poll = ParameterName.of("venue-notification-poll");

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public RiskyVenueHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK, new PrintingJsonEvents(SystemClock.CLOCK));
    }

    public RiskyVenueHandler(Environment environment, Supplier<Instant> clock, Events events) {
        this(
            environment,
            awsAuthentication(ApiName.Mobile, events),
            signResponseWithKeyGivenInSsm(environment, clock, events),
            new AwsSsmParameters(),
            awsAuthentication(ApiName.Health, events),
            events
        );
    }

    public RiskyVenueHandler(Environment environment, Authenticator authenticator, ResponseSigner signer, Parameters parameters, Authenticator healthAuthenticator, Events events) {
        this(environment,
            authenticator,
            signer,
            new CircuitBreakerService(
                parameters.ofEnum(initial.withPrefix(environment.access.required(EnvironmentKeys.SSM_CIRCUIT_BREAKER_BASE_NAME)), ApprovalStatus.class, ApprovalStatus.PENDING),
                parameters.ofEnum(poll.withPrefix(environment.access.required(EnvironmentKeys.SSM_CIRCUIT_BREAKER_BASE_NAME)), ApprovalStatus.class, ApprovalStatus.PENDING)
            ),
            events,
            healthAuthenticator);
    }

    public RiskyVenueHandler(Environment environment, Authenticator authenticator, ResponseSigner signer, CircuitBreakerService circuitBreakerService, Events events, Authenticator healthAuthenticator) {
        handler = withSignedResponses(
            events,
            environment,
            signer,
            routes(
                authorisedBy(authenticator,
                    path(POST, startsWith("/circuit-breaker/venue/request"),
                        r -> {
                            events.emit(getClass(), new CircuitBreakerVenueRequest());
                            return mapResultToResponse(circuitBreakerService.getApprovalToken());
                        }
                    )
                ),
                authorisedBy(authenticator,
                    path(GET, startsWith("/circuit-breaker/venue/resolution"),
                        r -> {
                            events.emit(getClass(), new CircuitBreakerVenueResolution());
                            return mapResultToResponse(circuitBreakerService.getResolution(r.getPath()));
                        }
                    )
                ),
                authorisedBy(healthAuthenticator,
                    path(POST, "/circuit-breaker/venue/health", r ->
                        HttpResponses.ok()
                    )
                )
            )
        );
    }

    private APIGatewayProxyResponseEvent mapResultToResponse(CircuitBreakerResult result) {
        if (result.type == CircuitBreakerResult.ResultType.ValidationError) {
            return HttpResponses.unprocessableEntity("validation error: Content type is not text/json");
        }
        if (result.type == CircuitBreakerResult.ResultType.MissingPollingTokenError) {
            return HttpResponses.unprocessableEntity("missing polling token error: Request was submitted without a polling token");
        }
        return HttpResponses.ok(result.responseBody);
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
