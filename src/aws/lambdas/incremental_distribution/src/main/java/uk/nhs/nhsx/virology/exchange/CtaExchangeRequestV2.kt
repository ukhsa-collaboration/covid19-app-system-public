package uk.nhs.nhsx.virology.exchange

import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.CtaToken

data class CtaExchangeRequestV2(val ctaToken: CtaToken, val country: Country)
