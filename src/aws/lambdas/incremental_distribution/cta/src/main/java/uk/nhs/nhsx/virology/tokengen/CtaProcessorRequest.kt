package uk.nhs.nhsx.virology.tokengen

import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult

data class CtaProcessorRequest(
    val testResult: TestResult,
    val testEndDate: TestEndDate,
    val testKit: TestKit,
    val numberOfTokens: Int
)
