package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Handler
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.*
import java.util.function.Predicate

abstract class RoutingHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(
        request: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {
        RequestContext.assignAwsRequestId(context.awsRequestId)

        val indifferentHeaders = TreeMap<String, String>(CASE_INSENSITIVE_ORDER)
            .apply { putAll(request.headers.orEmpty()) }

        return handler().invoke(request.apply { headers = indifferentHeaders }, context)
    }

    abstract fun handler(): Handler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>

    fun startsWith(path: String) = Predicate { candidate: String -> candidate.startsWith(path) }
}
