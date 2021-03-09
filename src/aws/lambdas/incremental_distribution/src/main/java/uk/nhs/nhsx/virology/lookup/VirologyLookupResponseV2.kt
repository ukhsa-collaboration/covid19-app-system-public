package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult

data class VirologyLookupResponseV2(
    val testEndDate: TestEndDate,
    val testResult: TestResult,
    val testKit: TestKit,
    val diagnosisKeySubmissionSupported: Boolean,
    val requiresConfirmatoryTest: Boolean
)
