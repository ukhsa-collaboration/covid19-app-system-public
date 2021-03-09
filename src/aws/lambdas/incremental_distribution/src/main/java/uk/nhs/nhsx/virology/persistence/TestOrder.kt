package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.virology.TestResultPollingToken

class TestOrder(
    val ctaToken: CtaToken,
    val downloadCounter: Int,
    val testResultPollingToken: TestResultPollingToken,
    val diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken
) {
    constructor(
        ctaToken: CtaToken,
        testResultPollingToken: TestResultPollingToken,
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken
    ) : this(ctaToken, 0, testResultPollingToken, diagnosisKeySubmissionToken)
}
