package uk.nhs.nhsx.testhelper

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.*

private fun APIGatewayProxyRequestEvent.initHeaders() = apply {
    if (headers == null) headers = TreeMap(CASE_INSENSITIVE_ORDER)
}

fun APIGatewayProxyRequestEvent.withMethod(method: HttpMethod): APIGatewayProxyRequestEvent =
    withHttpMethod(method.name)

fun APIGatewayProxyRequestEvent.withJson(json: String?) = withBody(json)
    .withHeader("Content-Type", "application/json")

fun APIGatewayProxyRequestEvent.withCsv(csv: String?) = withBody(csv)
    .withHeader("Content-Type", "text/csv")

fun APIGatewayProxyRequestEvent.withCustomOai(oai: String) = withHeader("x-custom-oai", oai)
fun APIGatewayProxyRequestEvent.withRequestId(id: String = UUID.randomUUID().toString()) = withHeader("Request-Id", id)
fun APIGatewayProxyRequestEvent.withRequestId(id: UUID) = withHeader("Request-Id", id.toString())
fun APIGatewayProxyRequestEvent.withBearerToken(token: String) = withHeader("Authorization", "Bearer $token")
fun APIGatewayProxyRequestEvent.withHeader(name: String, value: String) = initHeaders().apply { headers[name] = value }
fun APIGatewayProxyRequestEvent.build() = this

object ProxyRequestBuilder {
    fun request() = APIGatewayProxyRequestEvent().initHeaders()
}
