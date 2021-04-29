package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.HttpResponses.badRequest
import uk.nhs.nhsx.core.HttpResponses.created
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.HttpResponses.serviceUnavailable
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withSignedResponses
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest

class IsolationPaymentOrderHandler @JvmOverloads constructor(
    private val environment: Environment = Environment.fromSystem(),
    clock: Clock = SystemClock.CLOCK,
    private val events: Events = PrintingJsonEvents(clock),
    authenticator: Authenticator = awsAuthentication(Mobile, events),
    signer: ResponseSigner = StandardSigningFactory(
        clock,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).signResponseWithKeyGivenInSsm(environment, events),
    private val service: IsolationPaymentMobileService = isolationPaymentService(clock, environment, events),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events)
) : RoutingHandler() {


    private fun createToken(request: APIGatewayProxyRequestEvent) =
        if (environment.access.required(TOKEN_CREATION_ENABLED))
            Json.readJsonOrNull<TokenGenerationRequest>(request.body) {
                events(UnprocessableJson(it))
            }
                ?.let { created(toJson(service.handleIsolationPaymentOrder(it))) }
                ?: badRequest()
        else serviceUnavailable()

    private fun updateToken(request: APIGatewayProxyRequestEvent) =
        when {
            environment.access.required(TOKEN_CREATION_ENABLED) ->
                Json.readJsonOrNull<TokenUpdateRequest>(request.body) {
                    events(UnprocessableJson(it))
                }
                    ?.let { ok(toJson(service.handleIsolationPaymentUpdate(it))) }
                    ?: badRequest()
            else -> serviceUnavailable()
        }

    override fun handler() = handler

    companion object {
        private val ISOLATION_TOKEN_TABLE = EnvironmentKey.string("ISOLATION_PAYMENT_TOKENS_TABLE")
        private val ISOLATION_PAYMENT_WEBSITE = EnvironmentKey.string("ISOLATION_PAYMENT_WEBSITE")
        private val TOKEN_EXPIRY_IN_WEEKS = EnvironmentKey.integer("TOKEN_EXPIRY_IN_WEEKS")
        private val COUNTRIES_WHITELISTED = EnvironmentKey.strings("COUNTRIES_WHITELISTED")
        private val TOKEN_CREATION_ENABLED = EnvironmentKey.bool("TOKEN_CREATION_ENABLED")
        private val AUDIT_LOG_PREFIX = EnvironmentKey.string("AUDIT_LOG_PREFIX")

        private fun isolationPaymentService(
            clock: Clock,
            environment: Environment,
            events: Events
        ) = IsolationPaymentMobileService(
            clock, IpcTokenIdGenerator::getToken,
            IsolationPaymentPersistence(
                AmazonDynamoDBClientBuilder.defaultClient(),
                environment.access.required(ISOLATION_TOKEN_TABLE)
            ),
            environment.access.required(ISOLATION_PAYMENT_WEBSITE),
            environment.access.required(TOKEN_EXPIRY_IN_WEEKS),
            environment.access.required(COUNTRIES_WHITELISTED),
            environment.access.required(AUDIT_LOG_PREFIX),
            events
        )
    }

    private val handler = withSignedResponses(
        events,
        environment,
        signer,
        routes(
            authorisedBy(
                authenticator,
                path(POST, "/isolation-payment/ipc-token/create",
                    ApiGatewayHandler { r, _ -> createToken(r) })
            ),
            authorisedBy(
                authenticator,
                path(POST, "/isolation-payment/ipc-token/update", ApiGatewayHandler { r, _ ->
                    updateToken(r)
                })
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/isolation-payment/health", ApiGatewayHandler { _, _ -> ok() })
            )
        )
    )
}
