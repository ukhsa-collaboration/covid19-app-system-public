package uk.nhs.nhsx.virology.exchange

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import java.time.LocalDate

class CtaExchangeResponseV2Test {

    @Test
    fun `throws error for illegal state when shouldOfferFollowUpTest is true`() {
        expectCatching {
            CtaExchangeResponseV2(
                diagnosisKeySubmissionToken = DiagnosisKeySubmissionToken.of("random-token"),
                testResult = TestResult.Positive,
                testEndDate = TestEndDate.of(LocalDate.EPOCH),
                testKit = TestKit.LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false,
                confirmatoryDayLimit = null,
                shouldOfferFollowUpTest = true,
                venueHistorySharingSupported = false
            )
        }.isFailure().message.isEqualTo("requiresConfirmatoryTest/shouldOfferFollowUpTest illegal state")
    }

    @Test
    fun `throws error for illegal state when confirmatoryDayLimit is not null`() {
        expectCatching {
            CtaExchangeResponseV2(
                diagnosisKeySubmissionToken = DiagnosisKeySubmissionToken.of("random-token"),
                testResult = TestResult.Positive,
                testEndDate = TestEndDate.of(LocalDate.EPOCH),
                testKit = TestKit.LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false,
                confirmatoryDayLimit = 1,
                shouldOfferFollowUpTest = false,
                venueHistorySharingSupported = false
            )
        }.isFailure().message.isEqualTo("requiresConfirmatoryTest/confirmatoryDayLimit illegal state")
    }
}
