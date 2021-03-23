package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.Companion.fromSystem
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.routing.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.RoutingHandler
import uk.nhs.nhsx.core.routing.StandardHandlers
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.isolationpayment.model.IsolationRequest
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal

class IsolationPaymentUploadHandler(
    environment: Environment,
    authenticator: Authenticator,
    private val service: IsolationPaymentGatewayService,
    private val events: Events,
    healthAuthenticator: Authenticator
) : RoutingHandler() {
    @Suppress("unused")
    @JvmOverloads
    constructor(
        environment: Environment = fromSystem(),
        clock: Clock = SystemClock.CLOCK,
        events: Events = PrintingJsonEvents(SystemClock.CLOCK)
    ) : this(
        environment,
        StandardAuthentication.awsAuthentication(ApiName.IsolationPayment, events),
        isolationPaymentService(clock, environment, events),
        events,
        StandardAuthentication.awsAuthentication(ApiName.Health, events)
    )

    private fun consumeToken(request: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent =
        Jackson.readOrNull<IsolationRequest>(request.body) {
            events(UnprocessableJson(it))
        }
            ?.let { service.consumeIsolationToken(it.ipcToken) }
            ?.let { mapToResponse(it) }
            ?: HttpResponses.badRequest()

    private fun verifyToken(request: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent =
        Jackson.readOrNull<IsolationRequest>(request.body) {
            events(UnprocessableJson(it))
        }
            ?.let { service.verifyIsolationToken(it.ipcToken) }
            ?.let { mapToResponse(it) }
            ?: HttpResponses.badRequest()

    private fun mapToResponse(tokenResponse: IsolationResponse): APIGatewayProxyResponseEvent =
        if (tokenResponse.state == TokenStateExternal.EXT_INVALID.value) {
            HttpResponses.unprocessableEntityWithJson(Jackson.toJson(tokenResponse))
        } else
            HttpResponses.ok(Jackson.toJson(tokenResponse))

    override fun handler(): ApiGatewayHandler = handler

    companion object {
        private val ISOLATION_TOKEN_TABLE = EnvironmentKey.string("ISOLATION_PAYMENT_TOKENS_TABLE")
        private val AUDIT_LOG_PREFIX = EnvironmentKey.string("AUDIT_LOG_PREFIX")

        private fun isolationPaymentService(
            clock: Clock,
            environment: Environment,
            events: Events
        ) = IsolationPaymentGatewayService(
            clock, IsolationPaymentPersistence(
                AmazonDynamoDBClientBuilder.defaultClient(),
                environment.access.required(ISOLATION_TOKEN_TABLE)
            ), environment.access.required(AUDIT_LOG_PREFIX), events
        )
    }

    private val handler: ApiGatewayHandler = StandardHandlers.withoutSignedResponses(
        events,
        environment,
        Routing.routes(
            authorisedBy(
                authenticator,
                path(POST, "/isolation-payment/ipc-token/consume-token") { it, _ -> consumeToken(it) }),
            authorisedBy(
                authenticator,
                path(POST, "/isolation-payment/ipc-token/verify-token") { it, _ -> verifyToken(it) }),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/isolation-payment/health") { _, _ -> HttpResponses.ok() })
        )
    )
}
