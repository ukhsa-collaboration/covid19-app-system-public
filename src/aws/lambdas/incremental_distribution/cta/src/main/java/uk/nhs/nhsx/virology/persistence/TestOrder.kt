package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestResultPollingToken
import java.time.LocalDateTime

data class TestOrder(
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
    ) : this(
        ctaToken = ctaToken,
        downloadCounter = 0,
        testResultPollingToken = testResultPollingToken,
        diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
        expiryTimeToLive = expiryTimeToLive
    )
}
