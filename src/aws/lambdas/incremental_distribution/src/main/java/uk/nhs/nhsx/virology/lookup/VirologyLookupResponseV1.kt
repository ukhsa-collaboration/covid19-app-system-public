package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult

data class VirologyLookupResponseV1(val testEndDate: TestEndDate, val testResult: TestResult?, val testKit: TestKit)
