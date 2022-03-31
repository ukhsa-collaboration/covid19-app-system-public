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
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.aws.ssm.ofEnum
import uk.nhs.nhsx.core.events.CircuitBreakerExposureRequest
import uk.nhs.nhsx.core.events.CircuitBreakerExposureResolution
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.routing.Routing.Method.GET
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withSignedResponses

class ExposureNotificationHandler @JvmOverloads constructor(
    environment: Environment = Environment.unknown(),
    clock: Clock = CLOCK,
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

    private val handler = withSignedResponses(
        events = events,
        environment = environment,
        signer = signer,
        delegate = routes(
            authorisedBy(
                authenticator,
                path(POST, startsWith("/circuit-breaker/exposure-notification/request")) { r, _ ->
                    events(CircuitBreakerExposureRequest())
                    when (Json.readJsonOrNull<ExposureNotificationCircuitBreakerRequest>(r.body) {
                        events(UnprocessableJson(it))
                    }) {
                        null -> CircuitBreakerResult.validationError()
                        else -> circuitBreakerService.getApprovalToken()
                    }.toResponse()
                }
            ),
            authorisedBy(
                authenticator,
                path(GET, startsWith("/circuit-breaker/exposure-notification/resolution")) { r, _ ->
                    events(CircuitBreakerExposureResolution())
                    circuitBreakerService.getResolution(r.path).toResponse()
                }
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/circuit-breaker/exposure-notification/health") { _, _ -> ok() })
        )
    )

    override fun handler() = handler

    private fun CircuitBreakerResult.toResponse() = when (type) {
        ValidationError -> unprocessableEntity("validation error: Content type is not text/json")
        MissingPollingTokenError -> unprocessableEntity("missing polling token error: Request submitted without approval token")
        else -> ok(responseBody)
    }

    companion object {
        private val initial = ParameterName.of("exposure-notification-initial")
        private val poll = ParameterName.of("exposure-notification-poll")
    }
}
