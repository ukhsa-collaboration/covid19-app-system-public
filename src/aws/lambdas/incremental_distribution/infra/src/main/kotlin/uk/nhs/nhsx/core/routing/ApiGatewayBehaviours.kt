package uk.nhs.nhsx.core.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
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
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.UserAgent
import uk.nhs.nhsx.core.routing.Routing.RoutingHandler
import java.time.Duration
import java.util.*

val MAINTENANCE_MODE = EnvironmentKey.string("MAINTENANCE_MODE")
val CUSTOM_OAI = EnvironmentKey.string("custom_oai")

fun withoutSignedResponses(
    events: Events,
    environment: Environment,
    delegate: ApiGatewayHandler
) = defaultStack(events, environment, catchExceptions(events, delegate))

fun withSignedResponses(
    events: Events,
    environment: Environment,
    signer: ResponseSigner,
    delegate: ApiGatewayHandler
) = defaultStack(events, environment, signedBy(signer, catchExceptions(events, delegate)))

private fun defaultStack(
    events: Events,
    environment: Environment,
    handler: ApiGatewayHandler
) = loggingIncomingRequests(
    events = events,
    delegate = filteringWhileMaintenanceModeEnabled(
        events = events,
        environment = environment,
        delegate = requiringAuthorizationHeader(
            requiringCustomAccessIdentity(events, environment, handler)
        )
    ),
    clock = SystemClock.CLOCK
)

fun loggingIncomingRequests(
    events: Events,
    delegate: ApiGatewayHandler,
    clock: Clock
) = ApiGatewayHandler { r, context ->
    val keyName = ApiKeyExtractor(r.headers["authorization"])?.keyName ?: "none"
    val requestId = r.headers.getOrDefault("Request-Id", "none")
    val userAgent = userAgentFrom(r)

    val start = clock()
    var statusCode = 500
    try {
        delegate.invoke(r, context).also { statusCode = it.statusCode }
    } finally {
        val latency = Duration.between(start, clock())
        events(
            IncomingHttpRequest(
                uri = r.path,
                method = r.httpMethod,
                status = statusCode,
                latency = latency.toMillis(),
                userAgent = UserAgent.of(userAgent),
                requestId = requestId,
                apiKey = keyName,
                message = "Received http request: method=${r.httpMethod},path=${r.path},requestId=${requestId},apiKeyName=${keyName},userAgent=${userAgent},status=${statusCode},latency=${latency}"
            )
        )
    }
}

private fun userAgentFrom(r: APIGatewayProxyRequestEvent) = r.headers.getOrDefault("User-Agent", "none")

fun mobileAppVersionFrom(r: APIGatewayProxyRequestEvent) = UserAgent.of(userAgentFrom(r)).appVersion

fun mobileOSFrom(r: APIGatewayProxyRequestEvent) = UserAgent.of(userAgentFrom(r)).os ?: MobileOS.Unknown

fun filteringWhileMaintenanceModeEnabled(
    events: Events,
    environment: Environment,
    delegate: ApiGatewayHandler
) = when {
    environment.isMaintenanceModeEnabled() ->
        ApiGatewayHandler { _, _ ->
            events(RequestRejected("MAINTENANCE_MODE"))
            HttpResponses.serviceUnavailable()
        }
    else -> delegate
}

fun Environment.isMaintenanceModeEnabled() =
    access.required(MAINTENANCE_MODE)
        .lowercase(Locale.getDefault())
        .toBoolean()

fun requiringAuthorizationHeader(delegate: ApiGatewayHandler) = ApiGatewayHandler { r, context ->
    when {
        r.headers.containsKey("authorization") -> delegate.invoke(r, context)
        else -> HttpResponses.forbidden()
    }
}

fun requiringCustomAccessIdentity(
    events: Events,
    environment: Environment,
    delegate: ApiGatewayHandler
): ApiGatewayHandler =
    environment.access.required(CUSTOM_OAI).let { requiredOai: String ->
        ApiGatewayHandler { request, context ->
            if (requiredOai == request.headers["x-custom-oai"]) {
                delegate.invoke(request, context)
            } else {
                events(OAINotSet(request.httpMethod, request.path))
                HttpResponses.forbidden()
            }
        }
    }

fun authorisedBy(
    authenticator: Authenticator,
    routingHandler: RoutingHandler
) = object : DelegatingRoutingHandler(routingHandler) {
    override fun invoke(request: APIGatewayProxyRequestEvent, context: Context) =
        request.headers["authorization"]
            ?.takeIf(authenticator::isAuthenticated)
            ?.let { delegate.invoke(request, context) }
            ?: HttpResponses.forbidden()
}

fun signedBy(
    signer: ResponseSigner,
    delegate: ApiGatewayHandler
) = ApiGatewayHandler { request, context ->
    val response = delegate.invoke(request, context)
    if (response.statusCode != 403) {
        signer.sign(request, response)
    }
    response
}

fun catchExceptions(
    events: Events,
    delegate: ApiGatewayHandler
) = ApiGatewayHandler { r, context ->
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
