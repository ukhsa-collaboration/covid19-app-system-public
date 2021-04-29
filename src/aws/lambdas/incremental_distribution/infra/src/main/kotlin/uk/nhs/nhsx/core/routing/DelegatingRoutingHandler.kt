package uk.nhs.nhsx.core.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import uk.nhs.nhsx.core.routing.Routing.RouterMatch.Matched
import uk.nhs.nhsx.core.routing.Routing.RoutingHandler

abstract class DelegatingRoutingHandler protected constructor(val delegate: RoutingHandler) : RoutingHandler {
    override fun match(request: APIGatewayProxyRequestEvent) = when(val match = delegate.match(request)) {
        is Matched -> Matched(this)
        else -> match
    }
}
