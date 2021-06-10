package uk.nhs.nhsx.virology.result

import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult

data class VirologyResultRequestV1(
    val ctaToken: CtaToken,
    val testEndDate: TestEndDate,
    val testResult: TestResult
)
