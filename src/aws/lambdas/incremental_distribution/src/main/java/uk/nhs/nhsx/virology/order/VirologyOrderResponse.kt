package uk.nhs.nhsx.virology.order

import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.virology.TestResultPollingToken
import uk.nhs.nhsx.virology.persistence.TestOrder

data class VirologyOrderResponse(
    val websiteUrlWithQuery: String,
    val tokenParameterValue: CtaToken,
    val testResultPollingToken: TestResultPollingToken,
    val diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken
) {
    constructor(websiteUrlWithQuery: String, testOrder: TestOrder) : this(
        websiteUrlWithQuery,
        testOrder.ctaToken,
        testOrder.testResultPollingToken,
        testOrder.diagnosisKeySubmissionToken
    )
}
