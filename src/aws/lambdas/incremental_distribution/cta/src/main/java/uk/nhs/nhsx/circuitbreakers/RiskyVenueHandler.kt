package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.services.kms.AWSKMSClientBuilder
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.PENDING
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.MissingPollingTokenError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.ValidationError
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys.SSM_CIRCUIT_BREAKER_BASE_NAME
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.HttpResponses.unprocessableEntity
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.aws.ssm.ofEnum
import uk.nhs.nhsx.core.events.CircuitBreakerVenueRequest
import uk.nhs.nhsx.core.events.CircuitBreakerVenueResolution
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.routing.Routing.Method.GET
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withSignedResponses

class RiskyVenueHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = SystemClock.CLOCK,
    events: Events = PrintingJsonEvents(clock),
    authenticator: Authenticator = awsAuthentication(ApiName.Mobile, events),
    parameters: Parameters = AwsSsmParameters(),
    signer: ResponseSigner = StandardSigningFactory(
        clock,
        parameters,
        AWSKMSClientBuilder.defaultClient()
    ).signResponseWithKeyGivenInSsm(environment, events),
    circuitBreakerService: CircuitBreakerService = CircuitBreakerService(
        parameters.ofEnum(
            name = initial.withPrefix(environment.access.required(SSM_CIRCUIT_BREAKER_BASE_NAME)),
            type = ApprovalStatus::class.java,
            whenError = PENDING
        ),
        parameters.ofEnum(
            name = poll.withPrefix(environment.access.required(SSM_CIRCUIT_BREAKER_BASE_NAME)),
            type = ApprovalStatus::class.java,
            whenError = PENDING
        )
    ),
    healthAuthenticator: Authenticator = awsAuthentication(ApiName.Health, events)
) : RoutingHandler() {

    private val handler = withSignedResponses(
        events = events,
        environment = environment,
        signer = signer,
        delegate = routes(
            authorisedBy(
                authenticator,
                path(POST, startsWith("/circuit-breaker/venue/request")) { _, _ ->
                    events(CircuitBreakerVenueRequest())
                    circuitBreakerService.getApprovalToken().toResponse()
                }
            ),
            authorisedBy(
                authenticator,
                path(GET, startsWith("/circuit-breaker/venue/resolution")) { r, _ ->
                    events(CircuitBreakerVenueResolution())
                    circuitBreakerService.getResolution(r.path).toResponse()
                }
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/circuit-breaker/venue/health") { _, _ ->
                    ok()
                }
            )
        )
    )

    private fun CircuitBreakerResult.toResponse() = when (type) {
        ValidationError -> unprocessableEntity("validation error: Content type is not text/json")
        MissingPollingTokenError -> unprocessableEntity("missing polling token error: Request was submitted without a polling token")
        else -> ok(responseBody)
    }

    override fun handler() = handler

    companion object {
        private val initial = ParameterName.of("venue-notification-initial")
        private val poll = ParameterName.of("venue-notification-poll")
    }
}
