package uk.nhs.nhsx.virology.tokengen

import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult

data class CtaProcessorRequest(
    val testResult: TestResult,
    val testEndDate: TestEndDate,
    val testKit: TestKit,
    val numberOfTokens: Int
)
