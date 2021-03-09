package uk.nhs.nhsx.virology.lookup

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Jackson

abstract class VirologyLookupResult {
    abstract fun toHttpResponse(): APIGatewayProxyResponseEvent

    class Available(val response: VirologyLookupResponseV1) : VirologyLookupResult() {
        override fun toHttpResponse() = HttpResponses.ok(Jackson.toJson(response))
    }

    class AvailableV2(val response: VirologyLookupResponseV2) : VirologyLookupResult() {
        override fun toHttpResponse() = HttpResponses.ok(Jackson.toJson(response))
    }

    class Pending : VirologyLookupResult() {
        override fun toHttpResponse() = HttpResponses.noContent()
    }

    class NotFound : VirologyLookupResult() {
        override fun toHttpResponse() = HttpResponses.notFound(
            "Test result lookup submitted for unknown testResultPollingToken"
        )
    }
}
