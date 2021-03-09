package uk.nhs.nhsx.virology.exchange

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Jackson

abstract class CtaExchangeResult {
    abstract fun toHttpResponse(): APIGatewayProxyResponseEvent

    class Available(val ctaExchangeResponse: CtaExchangeResponseV1) : CtaExchangeResult() {
        override fun toHttpResponse() = HttpResponses.ok(Jackson.toJson(ctaExchangeResponse))
    }

    class AvailableV2(val ctaExchangeResponse: CtaExchangeResponseV2) : CtaExchangeResult() {
        override fun toHttpResponse() = HttpResponses.ok(Jackson.toJson(ctaExchangeResponse))
    }

    class Pending : CtaExchangeResult() {
        override fun toHttpResponse() = HttpResponses.noContent()
    }

    class NotFound : CtaExchangeResult() {
        override fun toHttpResponse() = HttpResponses.notFound()
    }
}
