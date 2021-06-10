package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.*
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.domain.*
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.tokengen.CtaProcessorResult
import uk.nhs.nhsx.virology.tokengen.CtaTokenZipFileEntryRequest

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

data class CtaTokensGenerationComplete(val event: CtaProcessorResult) : Event(Info)

class CtaTokensAndUrlGenerationCompleted(val message: String, val createdBatches:Int, val createdTokens:Int) : Event(Info)
class CtaTokensAndUrlGenerationFailed(val requests: List<CtaTokenZipFileEntryRequest>, val message: String) : Event(Error)

data class TestResultUploaded(
    val version: Int,
    val source: VirologyResultSource,
    val ctaToken: CtaToken,
    val testResult: TestResult,
    val testKit: TestKit
) : Event(Info)

data class CtaTokenGen(
    val version: Int,
    val source: VirologyTokenExchangeSource,
    val testResult: TestResult,
    val testKit: TestKit
) : Event(Info)

data class TokenStatusCheck(
    val version: Int,
    val source: VirologyTokenExchangeSource,
    val ctaToken: String,
) : Event(Info)

data class ConsumableTokenStatusCheck(
    val version: Int,
    val source: VirologyTokenExchangeSource,
    val testKit: TestKit
) : Event(Info)
