package uk.nhs.nhsx.virology.order

import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.virology.persistence.TestOrder

data class VirologyOrderResponse(
    val websiteUrlWithQuery: String,
    val tokenParameterValue: CtaToken,
    val testResultPollingToken: TestResultPollingToken,
    val diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
    val venueHistorySubmissionToken: String = ""
) {
    constructor(websiteUrlWithQuery: String, testOrder: TestOrder) : this(
        websiteUrlWithQuery,
        testOrder.ctaToken,
        testOrder.testResultPollingToken,
        testOrder.diagnosisKeySubmissionToken,
    )
}
