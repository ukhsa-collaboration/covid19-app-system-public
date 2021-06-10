package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestResultPollingToken

data class VirologyLookupRequestV2(val testResultPollingToken: TestResultPollingToken, val country: Country)
