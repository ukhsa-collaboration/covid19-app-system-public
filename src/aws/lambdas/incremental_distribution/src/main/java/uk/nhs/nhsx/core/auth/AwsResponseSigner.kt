package uk.nhs.nhsx.core.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.util.Base64
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.NoRequestIdFound
import uk.nhs.nhsx.core.signature.DatedSigner
import uk.nhs.nhsx.core.signature.SigningHeaders

class AwsResponseSigner(private val contentSigner: DatedSigner, private val events: Events) : ResponseSigner {

    override fun sign(request: APIGatewayProxyRequestEvent, response: APIGatewayProxyResponseEvent) {
        val method = request.httpMethod
        val path = request.path
        val body = response.body ?: ""
        val requestId = request.headers["Request-Id"] ?: "not-set"

        if (!request.headers.containsKey("Request-Id")) {
            events.emit(javaClass, NoRequestIdFound(request.path))
        }

        val signature = contentSigner.sign {
            if (response.isBase64Encoded == true) {
                "$requestId:$method:$path:${it.string}:".toByteArray() + Base64.decode(body)
            } else {
                "$requestId:$method:$path:${it.string}:$body".toByteArray()
            }
        }

        SigningHeaders.fromDatedSignature(signature).forEach {
            response.headers[it.asHttpHeaderName()] = it.value
        }
    }
}
