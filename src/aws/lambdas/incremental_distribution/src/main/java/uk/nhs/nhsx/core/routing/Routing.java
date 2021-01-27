package uk.nhs.nhsx.core.routing;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableList;
import uk.nhs.nhsx.core.HttpResponses;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Routing {

    public static List<RoutingHandler> listOf(RoutingHandler... routes) {
        return Arrays.asList(routes);
    }

    public static AggregateRoutingHttpHandler routes(RoutingHandler... routes) {
        return routes(Arrays.asList(routes), ImmutableList.of());
    }

    public static AggregateRoutingHttpHandler routes(List<RoutingHandler> first, List<RoutingHandler> second) {
        var allRoutes = Stream.concat(first.stream(), second.stream()).collect(toList());
        return new AggregateRoutingHttpHandler(allRoutes, routeNotFoundHandler, Routing.routeMethodNotAllowedHandler);
    }

    public static PathRoutingHandler path(String path, Handler handler) {
        return new PathRoutingHandler(path, handler);
    }

    public static PathRoutingHandler path(Method method, String path, Handler handler) {
        return new PathRoutingHandler(path, Optional.of(method), handler);
    }

    public static PathRoutingHandler path(Method method, Predicate<String> pathMatcher, Handler handler) {
        return new PathRoutingHandler(pathMatcher, Optional.of(method), handler);
    }

    public static List<Routing.RoutingHandler> includeIf(boolean condition, Routing.RoutingHandler... handlers) {
        return condition ? Arrays.asList(handlers) : ImmutableList.of();
    }

    public static APIGatewayProxyResponseEvent throttlingResponse(Duration throttleDuration,
                                                                  Supplier<APIGatewayProxyResponseEvent> responseSupplier) {
        var response = responseSupplier.get();

        try {
            Thread.sleep(throttleDuration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to delay response", e);
        }

        return response;
    }

    public interface Handler {
        APIGatewayProxyResponseEvent handle(APIGatewayProxyRequestEvent request);
    }

    public interface Router {
        RouterMatch match(APIGatewayProxyRequestEvent request);
    }

    public interface RoutingHandler extends Router, Handler {

    }

    public enum Match {Matched, MethodUnMatched, UnMatched}

    public enum Method {
        GET, PUT, POST, PATCH;

        public boolean matches(String method) {
            return name().equals(method);
        }
    }

    private static class RouterMatch implements Handler, Comparable<RouterMatch> {
        private final Match matchType;
        private final Handler handler;

        public RouterMatch(Match matchType, Handler handler) {
            this.matchType = matchType;
            this.handler = handler;
        }

        public RouterMatch(Match matchType) {
            this.matchType = matchType;
            this.handler = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RouterMatch that = (RouterMatch) o;
            return matchType == that.matchType && Objects.equals(handler, that.handler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(matchType, handler);
        }

        @Override
        public int compareTo(RouterMatch routerMatch) {
            return matchType.compareTo(routerMatch.matchType);
        }

        @Override
        public APIGatewayProxyResponseEvent handle(APIGatewayProxyRequestEvent request) {
            if (handler != null) {
                return handler.handle(request);
            }
            throw new IllegalStateException("programmer error - should only invoke on matched not " + matchType);
        }

        public static RouterMatch matched(Handler handler) {
            return new RouterMatch(Match.Matched, handler);
        }

        public static RouterMatch unmatched() {
            return new RouterMatch(Match.UnMatched);
        }

        public static RouterMatch methodUnmatched() {
            return new RouterMatch(Match.MethodUnMatched);
        }
    }

    private static final Handler routeNotFoundHandler = (r) -> HttpResponses.notFound();
    private static final Handler routeMethodNotAllowedHandler = (r) -> HttpResponses.methodNotAllowed();

    public static class AggregateRoutingHttpHandler implements RoutingHandler {
        private final List<RoutingHandler> list;
        private final Handler routeNotFoundHandler;
        private final Handler methodNotMatchedHandler;

        public AggregateRoutingHttpHandler(List<RoutingHandler> list, Handler routeNotFoundHandler, Handler methodNotMatchedHandler) {
            this.list = list;
            this.routeNotFoundHandler = routeNotFoundHandler;
            this.methodNotMatchedHandler = methodNotMatchedHandler;
        }

        @Override
        public APIGatewayProxyResponseEvent handle(APIGatewayProxyRequestEvent request) {
            RouterMatch match = match(request);
            switch (match.matchType) {
                case Matched:
                    return match.handle(request);
                case MethodUnMatched:
                    return methodNotMatchedHandler.handle(request);
                case UnMatched:
                default:
                    return routeNotFoundHandler.handle(request);
            }
        }

        @Override
        public RouterMatch match(APIGatewayProxyRequestEvent request) {
            return list.stream().map(r -> r.match(request))
                .sorted()
                .findFirst()
                .orElse(RouterMatch.unmatched());
        }
    }

    public static class PathRoutingHandler implements RoutingHandler {

        private final Predicate<String> pathMatcher;
        private final Optional<Method> method;
        private final Handler handler;

        public PathRoutingHandler(Predicate<String> pathMatcher, Optional<Method> method, Handler handler) {
            this.pathMatcher = pathMatcher;
            this.method = method;
            this.handler = handler;
        }

        public PathRoutingHandler(String path, Optional<Method> method, Handler handler) {
            this(path::contentEquals, method, handler);
        }

        public PathRoutingHandler(String path, Handler handler) {
            this(path::contentEquals, Optional.empty(), handler);
        }

        @Override
        public APIGatewayProxyResponseEvent handle(APIGatewayProxyRequestEvent request) {
            RouterMatch match = match(request);
            switch (match.matchType) {
                case Matched:
                    return match.handle(request);
                case MethodUnMatched:
                    return routeMethodNotAllowedHandler.handle(request);
                case UnMatched:
                default:
                    return routeNotFoundHandler.handle(request);
            }
        }

        @Override
        public RouterMatch match(APIGatewayProxyRequestEvent request) {
            if (pathMatcher.test(request.getPath())) {
                if (method.isPresent()) {
                    if (method.get().matches(request.getHttpMethod())) {
                        return RouterMatch.matched(handler);
                    } else {
                        return RouterMatch.methodUnmatched();
                    }
                }
                return RouterMatch.matched(handler);
            }
            return RouterMatch.unmatched();
        }
    }
}
