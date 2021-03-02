package uk.nhs.nhsx.core.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.apache.http.entity.ContentType
import org.apache.logging.log4j.LogManager
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.auth.ApiKeyExtractor
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.events.ApiHandleFailed
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.OAINotSet
import uk.nhs.nhsx.core.exceptions.ApiResponseException
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.UserAgent
import java.time.Duration
import java.util.Optional

object StandardHandlers {
    private val logger = LogManager.getLogger(StandardHandlers::class.java)
    private val MAINTENANCE_MODE = Environment.EnvironmentKey.string("MAINTENANCE_MODE")
    private val CUSTOM_OAI = Environment.EnvironmentKey.string("custom_oai")

    @JvmStatic
    fun withoutSignedResponses(events: Events, environment: Environment, delegate: Routing.Handler): Routing.Handler =
        defaultStack(events, environment, catchExceptions(events, delegate))

    @JvmStatic
    fun withSignedResponses(
        events: Events,
        environment: Environment,
        signer: ResponseSigner,
        delegate: Routing.Handler
    ): Routing.Handler = defaultStack(events, environment, signedBy(signer, catchExceptions(events, delegate)))

    private fun defaultStack(events: Events, environment: Environment, handler: Routing.Handler): Routing.Handler =
        loggingIncomingRequests(
            events,
            filteringWhileMaintenanceModeEnabled(
                environment,
                requiringAuthorizationHeader(
                    requiringCustomAccessIdentity(events, environment, handler)
                )
            )
        )

    private fun loggingIncomingRequests(events: Events, delegate: Routing.Handler): Routing.Handler =
        Routing.Handler { r: APIGatewayProxyRequestEvent ->
            val keyName = ApiKeyExtractor(r.headers["authorization"])?.keyName ?: "none"
            val requestId = Optional.ofNullable(r.headers["Request-Id"]).orElse("none")
            val userAgent = userAgentFrom(r)

            val start = System.currentTimeMillis()
            var statusCode = 500
            try {
                delegate.handle(r).also {
                    statusCode = it.statusCode
                }
            } finally {
                logger.info(
                    "Received http request: method={}, path={},requestId={},apiKeyName={},userAgent={},status={},latency={}",
                    r.httpMethod, r.path, requestId, keyName, userAgent, statusCode, Duration.ofMillis(System.currentTimeMillis() - start)
                )
            }
        }

    private fun userAgentFrom(r: APIGatewayProxyRequestEvent): String =
        Optional.ofNullable(r.headers["User-Agent"]).orElse("none")

    @JvmStatic
    fun mobileAppVersionFrom(r: APIGatewayProxyRequestEvent): MobileAppVersion =
        UserAgent(userAgentFrom(r)).mobileAppVersion()

    fun filteringWhileMaintenanceModeEnabled(environment: Environment, delegate: Routing.Handler): Routing.Handler =
        when {
            environment.access.required(MAINTENANCE_MODE).toLowerCase().toBoolean() ->
                Routing.Handler { HttpResponses.serviceUnavailable() }
            else -> delegate
        }

    fun requiringAuthorizationHeader(delegate: Routing.Handler): Routing.Handler =
        Routing.Handler { r: APIGatewayProxyRequestEvent ->
            Optional.ofNullable(r.headers["authorization"]).map { delegate.handle(r) }
                .orElse(HttpResponses.forbidden())
        }

    fun requiringCustomAccessIdentity(
        events: Events,
        environment: Environment,
        delegate: Routing.Handler
    ): Routing.Handler =
        environment.access.required(CUSTOM_OAI).let { requiredOai: String ->
            Routing.Handler { request: APIGatewayProxyRequestEvent ->
                if (requiredOai == request.headers["x-custom-oai"]) {
                    delegate.handle(request)
                } else {
                    events.emit(javaClass, OAINotSet(request.httpMethod, request.path))
                    HttpResponses.forbidden()
                }
            }
        }

    @JvmStatic
    fun authorisedBy(authenticator: Authenticator, routingHandler: Routing.RoutingHandler?): Routing.RoutingHandler =
        object : DelegatingRoutingHandler(routingHandler) {
            override fun handle(r: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent =
                Optional.ofNullable(r.headers["authorization"])
                    .filter { authorizationHeader -> authenticator.isAuthenticated(authorizationHeader) }
                    .map { delegate.handle(r) }
                    .orElse(HttpResponses.forbidden())
        }

    fun signedBy(signer: ResponseSigner, delegate: Routing.Handler): Routing.Handler =
        Routing.Handler { request: APIGatewayProxyRequestEvent ->
            val response = delegate.handle(request)
            if (response.statusCode != 403) {
                signer.sign(request, response)
            }
            response
        }

    fun expectingContentType(contentType: ContentType, handler: Routing.Handler): Routing.Handler =
        Routing.Handler { r: APIGatewayProxyRequestEvent ->
            val given = Optional.ofNullable(r.headers["Content-Type"])
            if (given.isPresent) {
                given
                    .filter { c -> contentType.mimeType == ContentType.parse(c).mimeType }
                    .map { handler.handle(r) }
                    .orElse(HttpResponses.unprocessableEntity())
            } else {
                HttpResponses.badRequest()
            }
        }

    fun catchExceptions(events: Events, delegate: Routing.Handler): Routing.Handler =
        Routing.Handler { r: APIGatewayProxyRequestEvent ->
            try {
                delegate.handle(r)
            } catch (e: ApiResponseException) {
                events.emit(javaClass, ApiHandleFailed(e.statusCode.code, e.message))
                HttpResponses.withStatusCodeAndBody(e.statusCode, e.message)
            } catch (e: Exception) {
                events.emit(javaClass, ExceptionThrown(e))
                HttpResponses.internalServerError()
            }
        }
}
