package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.tokengen.CtaProcessorResult

data class CtaUpdateOnExchangeFailure(
    val ctaToken: CtaToken,
    val pollingToken: TestResultPollingToken,
    val submissionToken: DiagnosisKeySubmissionToken
) : Event(Warning)

data class TestResultPersistenceFailure(
    val ctaToken: CtaToken,
    val exception: TransactionException
) : Event(Warning)

data class TestResultMarkForDeletionFailure(
    val pollingToken: TestResultPollingToken,
    val message: String = ""
) : Event(Warning)

data class CtaTokenNotFound(
    val ctaToken: CtaToken?,
    val pollingToken: TestResultPollingToken?,
    val submissionToken: DiagnosisKeySubmissionToken?
) : Event(Warning)

data class UnprocessableVirologyCtaExchange(val e: Exception) : Event(Info)
data class VirologyOrderNotFound(val ctaToken: CtaToken) : Event(Warning)

class CtaTokensGenerationComplete(val event: CtaProcessorResult) : Event(Info)

class TestResultUploaded(val version: Int, val source: VirologyResultSource, val ctaToken: CtaToken, val testResult: String, val testKit: TestKit) : Event(
    Info)

class CtaTokenGen(val version: Int, val source: VirologyTokenExchangeSource, val testResult: String, val testKit: TestKit) : Event(
    Info
)
