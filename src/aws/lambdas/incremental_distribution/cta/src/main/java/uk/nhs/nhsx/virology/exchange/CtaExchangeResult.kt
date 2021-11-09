package uk.nhs.nhsx.virology.exchange

import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.AvailableV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.AvailableV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Pending

sealed class CtaExchangeResult {

    data class AvailableV1(val ctaExchangeResponse: CtaExchangeResponseV1) : CtaExchangeResult()

    data class AvailableV2(val ctaExchangeResponse: CtaExchangeResponseV2) : CtaExchangeResult()

    object Pending : CtaExchangeResult()

    data class NotFound(val ctaToken: CtaToken) : CtaExchangeResult()
}

fun CtaExchangeResult.toHttpResponse() = when (this) {
    is AvailableV1 -> HttpResponses.ok(Json.toJson(ctaExchangeResponse))
    is AvailableV2 -> HttpResponses.ok(Json.toJson(ctaExchangeResponse))
    Pending -> HttpResponses.noContent()
    is NotFound -> HttpResponses.notFound()
}
