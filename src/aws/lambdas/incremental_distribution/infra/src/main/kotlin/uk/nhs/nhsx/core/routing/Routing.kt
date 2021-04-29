package uk.nhs.nhsx.core.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Match.Matched
import java.time.Duration
import java.util.*
import java.util.function.Predicate
import java.util.function.Supplier

object Routing {

    fun routes(vararg routes: RoutingHandler): AggregateRoutingHttpHandler = routes(listOf(*routes), listOf())

    fun routes(first: List<RoutingHandler>, second: List<RoutingHandler>) =
        AggregateRoutingHttpHandler(first + second, routeNotFoundHandler, routeMethodNotAllowedHandler)

    fun path(path: String, handler: ApiGatewayHandler): PathRoutingHandler = PathRoutingHandler(path, handler)

    fun path(method: Method, path: String, handler: ApiGatewayHandler): PathRoutingHandler =
        PathRoutingHandler(path, Optional.of(method), handler)

    fun path(method: Method, pathMatcher: Predicate<String>, handler: ApiGatewayHandler): PathRoutingHandler =
        PathRoutingHandler(pathMatcher, Optional.of(method), handler)

    fun throttlingResponse(
        throttleDuration: Duration,
        responseSupplier: Supplier<APIGatewayProxyResponseEvent>
    ): APIGatewayProxyResponseEvent {
        val response = responseSupplier.get()
        try {
            Thread.sleep(throttleDuration.toMillis())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to delay response", e)
        }
        return response
    }

    private val routeNotFoundHandler = ApiGatewayHandler { _, _ -> HttpResponses.notFound() }
    private val routeMethodNotAllowedHandler = ApiGatewayHandler { _, _ -> HttpResponses.methodNotAllowed() }

    interface Router {
        fun match(request: APIGatewayProxyRequestEvent): RouterMatch
    }

    interface RoutingHandler : Router, ApiGatewayHandler

    enum class Match {
        Matched, MethodUnMatched, UnMatched
    }

    enum class Method {
        GET, PUT, POST, PATCH;

        fun matches(method: String): Boolean = name == method
    }

    sealed class RouterMatch(protected val matchType: Match) : Comparable<RouterMatch> {
        data class Matched(val handler: ApiGatewayHandler) : RouterMatch(Matched) {
            override fun compareTo(other: RouterMatch): Int = matchType.compareTo(other.matchType)
        }

        object UnMatched : RouterMatch(Match.UnMatched) {
            override fun compareTo(other: RouterMatch): Int = matchType.compareTo(other.matchType)
        }

        object MethodUnMatched : RouterMatch(Match.MethodUnMatched) {
            override fun compareTo(other: RouterMatch): Int = matchType.compareTo(other.matchType)
        }
    }

    class AggregateRoutingHttpHandler(
        private val list: List<RoutingHandler>,
        private val routeNotFoundHandler: ApiGatewayHandler,
        private val methodNotMatchedHandler: ApiGatewayHandler
    ) : RoutingHandler {
        override fun invoke(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
            when (val match = match(request)) {
                is RouterMatch.Matched -> match.handler
                is RouterMatch.MethodUnMatched -> methodNotMatchedHandler
                is RouterMatch.UnMatched -> routeNotFoundHandler
            }.invoke(request, context)

        override fun match(request: APIGatewayProxyRequestEvent): RouterMatch {
            return list.stream().map { r: RoutingHandler -> r.match(request) }
                .sorted()
                .findFirst()
                .orElse(RouterMatch.UnMatched)
        }
    }

    class PathRoutingHandler(
        private val pathMatcher: Predicate<String>,
        private val method: Optional<Method>,
        private val handler: ApiGatewayHandler
    ) : RoutingHandler {

        constructor(path: String, method: Optional<Method>, handler: ApiGatewayHandler) : this(
            Predicate<String> { cs: String -> path.contentEquals(cs) }, method, handler
        )

        constructor(path: String, handler: ApiGatewayHandler) : this(Predicate<String> { cs: String ->
            path.contentEquals(cs)
        }, Optional.empty(), handler)

        override fun invoke(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
            when (val match = match(request)) {
                is RouterMatch.Matched -> match.handler
                is RouterMatch.MethodUnMatched -> routeMethodNotAllowedHandler
                is RouterMatch.UnMatched -> routeNotFoundHandler
            }(request, context)

        override fun match(request: APIGatewayProxyRequestEvent): RouterMatch {
            return if (pathMatcher.test(request.path)) {
                if (method.isPresent) {
                    if (method.get().matches(request.httpMethod)) {
                        RouterMatch.Matched(handler)
                    } else {
                        RouterMatch.MethodUnMatched
                    }
                } else RouterMatch.Matched(handler)
            } else RouterMatch.UnMatched
        }
    }
}
