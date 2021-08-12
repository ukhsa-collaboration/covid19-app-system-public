package uk.nhs.nhsx.testhelper.assertions

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.Status
import strikt.api.Assertion

object AwsRuntimeAssertions {

    object ProxyResponse {
        val Assertion.Builder<APIGatewayProxyResponseEvent>.status get() = get(APIGatewayProxyResponseEvent::getStatusCode)
        val Assertion.Builder<APIGatewayProxyResponseEvent>.headers get() = get(APIGatewayProxyResponseEvent::getHeaders)
        val Assertion.Builder<APIGatewayProxyResponseEvent>.body get() = get(APIGatewayProxyResponseEvent::getBody)

        fun Assertion.Builder<APIGatewayProxyResponseEvent>.hasStatus(expected: Status) = and {
            assertThat("has status %s", expected) {
                it.statusCode == expected.code
            }
        }
    }

    object ProxyRequest {
        val Assertion.Builder<APIGatewayProxyRequestEvent>.method get() = get(APIGatewayProxyRequestEvent::getHttpMethod)
        val Assertion.Builder<APIGatewayProxyRequestEvent>.path get() = get(APIGatewayProxyRequestEvent::getPath)
        val Assertion.Builder<APIGatewayProxyRequestEvent>.headers get() = get(APIGatewayProxyRequestEvent::getHeaders)
        val Assertion.Builder<APIGatewayProxyRequestEvent>.body get() = get(APIGatewayProxyRequestEvent::getBody)
    }
}

