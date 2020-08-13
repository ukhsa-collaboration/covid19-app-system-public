package uk.nhs.nhsx.circuitbreakers;

import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import static uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.startsWith;
import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

/**
 * Lambda Facade for Exposure Notification Circuit Breakers
 * <p>
 * doc/design/api-contracts/exposure-notification-circuit-breaker.md
 * <p>
 */
public class ExposureNotificationHandler extends RoutingHandler {

    private final Routing.Handler handler;

    public ExposureNotificationHandler() {
        this(awsAuthentication(ApiName.Mobile), signResponseWithKeyGivenInSsm(SystemClock.CLOCK, Environment.unknown()));
    }

    public ExposureNotificationHandler(Authenticator authenticator, ResponseSigner signer) {
        CircuitBreakerService circuitBreakerService = new CircuitBreakerService();
        this.handler = withSignedResponses(authenticator, signer, routes(
            path(
                Routing.Method.POST,
                startsWith("/circuit-breaker/exposure-notification/request"),
                r -> deserializeMaybe(r.getBody(), ExposureNotificationCircuitBreakerRequest.class)
                    .map(it -> circuitBreakerService.getApprovalToken())
                    .orElseThrow(() -> new ApiResponseException(UNPROCESSABLE_ENTITY_422))),
            path(
                Routing.Method.GET,
                startsWith("/circuit-breaker/exposure-notification/resolution"),
                r -> circuitBreakerService.getResolution(r.getPath()))
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

}
