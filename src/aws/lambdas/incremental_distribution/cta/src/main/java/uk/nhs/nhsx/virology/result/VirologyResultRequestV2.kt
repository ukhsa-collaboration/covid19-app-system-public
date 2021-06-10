package uk.nhs.nhsx.virology.result

import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.virology.result.VirologyTestKitValidator.validate

data class VirologyResultRequestV2(
    val ctaToken: CtaToken,
    val testEndDate: TestEndDate,
    val testResult: TestResult,
    val testKit: TestKit
) {
    init {
        validate(testKit, testResult)
    }
}

fun VirologyResultRequestV1.convertToV2(): VirologyResultRequestV2 =
    VirologyResultRequestV2(
        ctaToken,
        testEndDate,
        testResult,
        LAB_RESULT
    )
