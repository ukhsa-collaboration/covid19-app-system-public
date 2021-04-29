package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.ParameterName.Companion.of
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.aws.ssm.ofEnum
import uk.nhs.nhsx.core.events.CircuitBreakerVenueRequest
import uk.nhs.nhsx.core.events.CircuitBreakerVenueResolution
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.handler.RoutingHandler
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
            initial.withPrefix(environment.access.required(EnvironmentKeys.SSM_CIRCUIT_BREAKER_BASE_NAME)),
            ApprovalStatus::class.java,
            ApprovalStatus.PENDING
        ),
        parameters.ofEnum(
            poll.withPrefix(environment.access.required(EnvironmentKeys.SSM_CIRCUIT_BREAKER_BASE_NAME)),
            ApprovalStatus::class.java,
            ApprovalStatus.PENDING
        )
    ),
    healthAuthenticator: Authenticator = awsAuthentication(ApiName.Health, events)
) : RoutingHandler() {
    private val handler: ApiGatewayHandler

    private fun mapResultToResponse(result: CircuitBreakerResult): APIGatewayProxyResponseEvent {
        if (result.type == CircuitBreakerResult.ResultType.ValidationError) {
            return HttpResponses.unprocessableEntity("validation error: Content type is not text/json")
        }
        return if (result.type == CircuitBreakerResult.ResultType.MissingPollingTokenError) {
            HttpResponses.unprocessableEntity("missing polling token error: Request was submitted without a polling token")
        } else HttpResponses.ok(result.responseBody)
    }

    override fun handler(): ApiGatewayHandler {
        return handler
    }

    companion object {
        private val initial = of("venue-notification-initial")
        private val poll = of("venue-notification-poll")
    }

    init {
        handler = withSignedResponses(
            events,
            environment,
            signer,
            routes(
                authorisedBy(
                    authenticator,
                    path(
                        Routing.Method.POST, CircuitBreakerService.startsWith("/circuit-breaker/venue/request"),
                        ApiGatewayHandler { _, _ ->
                            events(CircuitBreakerVenueRequest())
                            mapResultToResponse(circuitBreakerService.getApprovalToken())
                        }
                    )
                ),
                authorisedBy(
                    authenticator,
                    path(
                        Routing.Method.GET, CircuitBreakerService.startsWith("/circuit-breaker/venue/resolution"),
                        ApiGatewayHandler { r, _ ->
                            events(CircuitBreakerVenueResolution())
                            mapResultToResponse(circuitBreakerService.getResolution(r.path))
                        }
                    )
                ),
                authorisedBy(
                    healthAuthenticator,
                    path(
                        Routing.Method.POST,
                        "/circuit-breaker/venue/health",
                        ApiGatewayHandler { _: APIGatewayProxyRequestEvent, _: Context -> HttpResponses.ok() }
                    )
                )
            )
        )
    }
}
