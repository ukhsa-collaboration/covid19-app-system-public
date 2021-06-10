package uk.nhs.nhsx.virology.result

import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult

data class VirologyTokenGenRequestV1 (
    val testEndDate: TestEndDate,
    val testResult: TestResult
)
