package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Handler
import java.util.Optional
import java.util.TreeMap

abstract class RoutingHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    override fun handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        RequestContext.assignAwsRequestId(context.awsRequestId)
        val insensitiveHeaders = TreeMap<String, String>(java.lang.String.CASE_INSENSITIVE_ORDER)
        Optional.ofNullable(request.headers)
            .ifPresent { map: Map<String, String>? ->
                insensitiveHeaders.putAll(
                    map!!
                )
            }
        request.headers = insensitiveHeaders
        return handler().invoke(request, context)
    }

    abstract fun handler(): Handler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
}
