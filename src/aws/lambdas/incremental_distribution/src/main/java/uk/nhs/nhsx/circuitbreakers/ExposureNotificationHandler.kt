package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.PENDING
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.MissingPollingTokenError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.ValidationError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.startsWith
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys.SSM_CIRCUIT_BREAKER_BASE_NAME
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.HttpResponses.unprocessableEntity
import uk.nhs.nhsx.core.Jackson.readOrNull
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.ParameterName.Companion.of
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.aws.ssm.ofEnum
import uk.nhs.nhsx.core.events.CircuitBreakerExposureRequest
import uk.nhs.nhsx.core.events.CircuitBreakerExposureResolution
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.routing.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Method.GET
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.RoutingHandler
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses
import java.time.Instant
import java.util.function.Supplier

class ExposureNotificationHandler @JvmOverloads constructor(
    environment: Environment = Environment.unknown(),
    clock: Supplier<Instant> = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    authenticator: Authenticator = awsAuthentication(Mobile, events),
    parameters: Parameters = AwsSsmParameters(),
    signer: ResponseSigner = StandardSigningFactory(
        clock,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).signResponseWithKeyGivenInSsm(environment, events),
    circuitBreakerService: CircuitBreakerService = CircuitBreakerService(
        parameters.ofEnum(
            initial.withPrefix(environment.access.required(SSM_CIRCUIT_BREAKER_BASE_NAME)),
            ApprovalStatus::class.java,
            PENDING
        ),
        parameters.ofEnum(
            poll.withPrefix(environment.access.required(SSM_CIRCUIT_BREAKER_BASE_NAME)),
            ApprovalStatus::class.java,
            PENDING
        )
    ),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events)
) : RoutingHandler() {

    private fun requestHandler(events: Events, circuitBreakerService: CircuitBreakerService) =
        ApiGatewayHandler { r, _ ->
            events.emit(javaClass, CircuitBreakerExposureRequest())
            mapResultToResponse(
                readOrNull<ExposureNotificationCircuitBreakerRequest>(r.body) {
                    events.emit(javaClass, UnprocessableJson(it))
                }
                    ?.let { circuitBreakerService.approvalToken }
                    ?: CircuitBreakerResult.validationError()
            )
        }

    private fun mapResultToResponse(result: CircuitBreakerResult): APIGatewayProxyResponseEvent =
        when (result.type) {
            ValidationError -> unprocessableEntity("validation error: Content type is not text/json")
            MissingPollingTokenError -> unprocessableEntity("missing polling token error: Request submitted without approval token")
            else -> ok(result.responseBody)
        }

    override fun handler() = handler

    private val handler = withSignedResponses(
        events,
        environment,
        signer,
        routes(
            authorisedBy(
                authenticator,
                path(
                    POST, startsWith("/circuit-breaker/exposure-notification/request"),
                    requestHandler(events, circuitBreakerService)
                )
            ),
            authorisedBy(
                authenticator,
                path(GET, startsWith("/circuit-breaker/exposure-notification/resolution"),
                    ApiGatewayHandler { r, _ ->
                        events.emit(javaClass, CircuitBreakerExposureResolution())
                        val result = circuitBreakerService.getResolution(r.path)
                        mapResultToResponse(result)
                    }
                )
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/circuit-breaker/exposure-notification/health",
                    ApiGatewayHandler { _, _ -> ok() }
                ))
        )
    )

    companion object {
        private val initial = of("exposure-notification-initial")
        private val poll = of("exposure-notification-poll")
    }
}
