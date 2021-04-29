package uk.nhs.nhsx.core.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

fun interface ResponseSigner {
    fun sign(request: APIGatewayProxyRequestEvent, response: APIGatewayProxyResponseEvent)
}
