package uk.nhs.nhsx.virology.result

data class VirologyTokenGenRequestV1 (
    val testEndDate: TestEndDate,
    val testResult: TestResult
)
