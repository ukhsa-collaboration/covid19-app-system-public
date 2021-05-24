package uk.nhs.nhsx.virology.exchange

import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult

data class CtaExchangeResponseV2(
    val diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
    val testResult: TestResult,
    val testEndDate: TestEndDate,
    val testKit: TestKit,
    val diagnosisKeySubmissionSupported: Boolean,
    val requiresConfirmatoryTest: Boolean,
    val confirmatoryDayLimit: Int?,
    val venueHistorySharingSupported: Boolean = false
)
