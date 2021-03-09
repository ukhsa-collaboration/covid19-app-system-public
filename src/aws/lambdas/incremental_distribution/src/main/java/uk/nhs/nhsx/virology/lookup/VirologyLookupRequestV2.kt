package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.virology.Country
import uk.nhs.nhsx.virology.TestResultPollingToken

data class VirologyLookupRequestV2(val testResultPollingToken: TestResultPollingToken, val country: Country)
