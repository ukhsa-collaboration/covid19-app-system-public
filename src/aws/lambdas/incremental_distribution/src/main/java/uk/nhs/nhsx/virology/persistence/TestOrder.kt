package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestResultPollingToken

class TestOrder(
    val ctaToken: CtaToken,
    val downloadCounter: Int,
    val testResultPollingToken: TestResultPollingToken,
    val diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,

    ) {
    constructor(
        ctaToken: CtaToken,
        testResultPollingToken: TestResultPollingToken,
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken
    ) : this(ctaToken, 0, testResultPollingToken, diagnosisKeySubmissionToken)
}
