package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult

data class VirologyLookupResponseV1(val testEndDate: TestEndDate, val testResult: TestResult?, val testKit: TestKit)
