package uk.nhs.nhsx.core.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Handler

fun interface ApiGatewayHandler : Handler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
