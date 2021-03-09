package uk.nhs.nhsx.virology.exchange

import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult

data class CtaExchangeResponseV1(
    val diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
    val testResult: TestResult,
    val testEndDate: TestEndDate,
    val testKit: TestKit
)
