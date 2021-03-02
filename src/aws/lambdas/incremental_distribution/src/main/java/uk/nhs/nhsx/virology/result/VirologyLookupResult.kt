package uk.nhs.nhsx.virology.result

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponse
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2

abstract class VirologyLookupResult {
    abstract fun toHttpResponse(): APIGatewayProxyResponseEvent
    class Available(val virologyLookupResponse: VirologyLookupResponse) : VirologyLookupResult() {
        override fun toHttpResponse(): APIGatewayProxyResponseEvent =
            HttpResponses.ok(Jackson.toJson(virologyLookupResponse))
    }

    class AvailableV2(val virologyLookupResponse: VirologyLookupResponseV2) : VirologyLookupResult() {
        override fun toHttpResponse(): APIGatewayProxyResponseEvent =
            HttpResponses.ok(Jackson.toJson(virologyLookupResponse))
    }

    class Pending : VirologyLookupResult() {
        override fun toHttpResponse(): APIGatewayProxyResponseEvent = HttpResponses.noContent()
    }

    class NotFound : VirologyLookupResult() {
        override fun toHttpResponse(): APIGatewayProxyResponseEvent = HttpResponses.notFound(
            "Test result lookup submitted for unknown testResultPollingToken"
        )
    }
}
