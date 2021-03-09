package uk.nhs.nhsx.virology.exchange

import uk.nhs.nhsx.virology.Country
import uk.nhs.nhsx.virology.CtaToken

data class CtaExchangeRequestV2(val ctaToken: CtaToken, val country: Country)
