package uk.nhs.nhsx.core.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiKeyExtractor
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.events.ApiHandleFailed
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.IncomingHttpRequest
import uk.nhs.nhsx.core.events.OAINotSet
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.exceptions.ApiResponseException
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.UserAgent
import java.time.Duration
import java.util.*

val MAINTENANCE_MODE = EnvironmentKey.string("MAINTENANCE_MODE")
val CUSTOM_OAI = EnvironmentKey.string("custom_oai")

fun withoutSignedResponses(
    events: Events,
    environment: Environment,
    delegate: ApiGatewayHandler
): ApiGatewayHandler =
    defaultStack(events, environment, catchExceptions(events, delegate))

fun withSignedResponses(
    events: Events,
    environment: Environment,
    signer: ResponseSigner,
    delegate: ApiGatewayHandler
): ApiGatewayHandler = defaultStack(events, environment, signedBy(signer, catchExceptions(events, delegate)))

private fun defaultStack(events: Events, environment: Environment, handler: ApiGatewayHandler): ApiGatewayHandler =
    loggingIncomingRequests(
        events,
        filteringWhileMaintenanceModeEnabled(
            events,
            environment,
            requiringAuthorizationHeader(
                requiringCustomAccessIdentity(events, environment, handler)
            )
        ),
        SystemClock.CLOCK
    )

fun loggingIncomingRequests(
    events: Events,
    delegate: ApiGatewayHandler,
    clock: Clock
): ApiGatewayHandler =
    ApiGatewayHandler { r: APIGatewayProxyRequestEvent, context ->
        val keyName = ApiKeyExtractor(r.headers["authorization"])?.keyName ?: "none"
        val requestId = Optional.ofNullable(r.headers["Request-Id"]).orElse("none")
        val userAgent = userAgentFrom(r)

        val start = clock()
        var statusCode = 500
        try {
            delegate.invoke(r, context).also { statusCode = it.statusCode }
        } finally {
            val latency = Duration.between(start, clock())
            events(
                IncomingHttpRequest(
                    r.path,
                    r.httpMethod,
                    statusCode,
                    latency.toMillis(),
                    UserAgent.of(userAgent),
                    requestId,
                    keyName,
                    "Received http request: method=${r.httpMethod},path=${r.path},requestId=${requestId},apiKeyName=${keyName},userAgent=${userAgent},status=${statusCode},latency=${latency}"
                )
            )
        }
    }

private fun userAgentFrom(r: APIGatewayProxyRequestEvent): String =
    Optional.ofNullable(r.headers["User-Agent"]).orElse("none")

fun mobileAppVersionFrom(r: APIGatewayProxyRequestEvent): MobileAppVersion =
    UserAgent.of(userAgentFrom(r)).appVersion

fun mobileOSFrom(r: APIGatewayProxyRequestEvent): MobileOS =
    UserAgent.of(userAgentFrom(r)).os ?: MobileOS.Unknown

fun filteringWhileMaintenanceModeEnabled(
    events: Events,
    environment: Environment,
    delegate: ApiGatewayHandler
): ApiGatewayHandler =
    when {
        environment.isMaintenanceModeEnabled() ->
            ApiGatewayHandler { _, _ ->
                events(RequestRejected("MAINTENANCE_MODE"))
                HttpResponses.serviceUnavailable()
            }
        else -> delegate
    }

fun Environment.isMaintenanceModeEnabled(): Boolean =
    access.required(MAINTENANCE_MODE)
        .lowercase(Locale.getDefault())
        .toBoolean()

fun requiringAuthorizationHeader(delegate: ApiGatewayHandler): ApiGatewayHandler =
    ApiGatewayHandler { r: APIGatewayProxyRequestEvent, context ->
        Optional.ofNullable(r.headers["authorization"]).map { delegate.invoke(r, context) }
            .orElse(HttpResponses.forbidden())
    }

fun requiringCustomAccessIdentity(
    events: Events,
    environment: Environment,
    delegate: ApiGatewayHandler
): ApiGatewayHandler =
    environment.access.required(CUSTOM_OAI).let { requiredOai: String ->
        ApiGatewayHandler { request: APIGatewayProxyRequestEvent, context ->
            if (requiredOai == request.headers["x-custom-oai"]) {
                delegate.invoke(request, context)
            } else {
                events(OAINotSet(request.httpMethod, request.path))
                HttpResponses.forbidden()
            }
        }
    }

fun authorisedBy(authenticator: Authenticator, routingHandler: Routing.RoutingHandler): Routing.RoutingHandler =
    object : DelegatingRoutingHandler(routingHandler) {
        override fun invoke(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
            Optional.ofNullable(request.headers["authorization"])
                .filter { authorizationHeader -> authenticator.isAuthenticated(authorizationHeader) }
                .map { delegate.invoke(request, context) }
                .orElse(HttpResponses.forbidden())
    }

fun signedBy(signer: ResponseSigner, delegate: ApiGatewayHandler): ApiGatewayHandler =
    ApiGatewayHandler { request: APIGatewayProxyRequestEvent, context ->
        val response = delegate.invoke(request, context)
        if (response.statusCode != 403) {
            signer.sign(request, response)
        }
        response
    }

fun catchExceptions(events: Events, delegate: ApiGatewayHandler): ApiGatewayHandler =
    ApiGatewayHandler { r: APIGatewayProxyRequestEvent, context ->
        try {
            delegate.invoke(r, context)
        } catch (e: ApiResponseException) {
            events(ApiHandleFailed(e.statusCode.code, e.message))
            HttpResponses.withStatusCodeAndBody(e.statusCode, e.message)
        } catch (e: Exception) {
            events(ExceptionThrown(e))
            HttpResponses.internalServerError()
        }
    }
