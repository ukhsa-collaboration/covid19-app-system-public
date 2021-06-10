package uk.nhs.nhsx.virology.result

import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.virology.result.VirologyTestKitValidator.validate

data class VirologyTokenGenRequestV2(
    val testEndDate: TestEndDate,
    val testResult: TestResult,
    val testKit: TestKit
) {
    init {
        validate(testKit, testResult)
    }
}

fun VirologyTokenGenRequestV1.convertToV2(): VirologyTokenGenRequestV2 =
    VirologyTokenGenRequestV2(this.testEndDate, this.testResult, LAB_RESULT)
