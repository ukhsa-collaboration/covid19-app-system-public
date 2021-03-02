package uk.nhs.nhsx.core.routing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Optional;
import java.util.TreeMap;

public abstract class RoutingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        TreeMap<String, String> insensitiveHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Optional.ofNullable(request.getHeaders())
            .ifPresent(insensitiveHeaders::putAll);

        request.setHeaders(insensitiveHeaders);

        return handler().handle(request);
    }

    public abstract Routing.Handler handler();
}
