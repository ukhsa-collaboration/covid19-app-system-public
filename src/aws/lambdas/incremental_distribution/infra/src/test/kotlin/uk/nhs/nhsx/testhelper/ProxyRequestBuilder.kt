package uk.nhs.nhsx.testhelper

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import java.util.Base64
import java.util.TreeMap
import java.util.UUID

class ProxyRequestBuilder {

    private val headers: MutableMap<String, String> = TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER)
    private var method: HttpMethod? = null
    private var path: String? = null
    private var body: String? = null
    private var byteBody: ByteArray? = null

    fun withMethod(method: HttpMethod): ProxyRequestBuilder {
        this.method = method
        return this
    }

    fun withPath(path: String): ProxyRequestBuilder {
        this.path = path
        return this
    }

    fun withJson(json: String?): ProxyRequestBuilder =
        withBody(json)
            .withHeader("Content-Type", "application/json")

    fun withCsv(csv: String): ProxyRequestBuilder =
        withBody(csv)
            .withHeader("Content-Type", "text/csv")

    fun withCustomOai(oai: String): ProxyRequestBuilder =
        withHeader("x-custom-oai", oai)

    fun withRequestId(id: String = UUID.randomUUID().toString()): ProxyRequestBuilder =
        withHeader("Request-Id", id)

    fun withBearerToken(token: String): ProxyRequestBuilder =
        withHeader("Authorization", "Bearer $token")

    fun withHeader(name: String, value: String): ProxyRequestBuilder {
        headers[name] = value
        return this
    }

    fun withBody(body: String?): ProxyRequestBuilder {
        this.body = body
        return this
    }

    fun build(): APIGatewayProxyRequestEvent {
        val event = APIGatewayProxyRequestEvent()
        if (method != null) event.httpMethod = method!!.name
        event.path = path
        if (byteBody != null) {
            event.body = Base64.getEncoder().encodeToString(byteBody)
            event.isBase64Encoded = true
        } else {
            event.body = body
            event.isBase64Encoded = false
        }
        event.headers = headers
        return event
    }

    companion object {
        fun request(): ProxyRequestBuilder = ProxyRequestBuilder()
    }
}
