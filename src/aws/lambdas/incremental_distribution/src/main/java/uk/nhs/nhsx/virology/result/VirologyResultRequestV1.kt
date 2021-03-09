package uk.nhs.nhsx.virology.result

import uk.nhs.nhsx.virology.CtaToken

data class VirologyResultRequestV1(
    val ctaToken: CtaToken,
    val testEndDate: TestEndDate,
    val testResult: TestResult
)
