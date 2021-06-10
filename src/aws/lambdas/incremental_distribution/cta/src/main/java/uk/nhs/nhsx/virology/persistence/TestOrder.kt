package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestResultPollingToken
import java.time.LocalDateTime

class TestOrder(
    val ctaToken: CtaToken,
    val downloadCounter: Int,
    val testResultPollingToken: TestResultPollingToken,
    val diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
    val expiryTimeToLive: LocalDateTime
) {
    constructor(
        ctaToken: CtaToken,
        testResultPollingToken: TestResultPollingToken,
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
        expiryTimeToLive: LocalDateTime
    ) : this(ctaToken, 0, testResultPollingToken, diagnosisKeySubmissionToken, expiryTimeToLive)
}
