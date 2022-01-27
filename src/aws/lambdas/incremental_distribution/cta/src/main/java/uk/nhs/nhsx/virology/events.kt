package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Error
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.domain.TokenAgeRange
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
    val message: String? = ""
) : Event(Warning)

data class VirologyOrderNotFound(val ctaToken: CtaToken) : Event(Warning)

data class CtaTokensGenerationComplete(val event: CtaProcessorResult) : Event(Info)

class CtaTokensAndUrlGenerationCompleted(
    val message: String,
    val createdBatches: Int,
    val createdTokens: Int
) : Event(Info)

class CtaTokensAndUrlGenerationFailed(
    val requests: List<CtaTokenZipFileEntryRequest>,
    val message: String
) : Event(Error)

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

data class VirologyResultPending(val ctaToken: CtaToken) : Event(Info)

object CtaExchangeRejectionEvent {
    val label = "label" to "CtaExchangeRejectionEvent"

    data class TestOrderNotFound(val ctaToken: CtaToken) : Event(Info, label)

    data class TestResultNotFound(
        val ctaToken: CtaToken,
        val testResultPollingToken: TestResultPollingToken
    ) : Event(Info, label)

    data class DownloadCountExceeded(
        val ctaToken: CtaToken,
        val downloadCounter: Int
    ) : Event(Info, label)

    data class PolicyRejectionV1(
        val ctaToken: CtaToken,
        val testKit: TestKit
    ) : Event(Info, label)

    data class PolicyRejectionV2(
        val ctaToken: CtaToken,
        val testKit: TestKit,
        val country: Country,
        val mobileAppVersion: MobileAppVersion,
        val mobileOS: MobileOS
    ) : Event(Info, label)

    data class UnprocessableVirologyCtaExchange(val e: Exception) : Event(Info, label)
}

// used by analytics
data class SuccessfulCtaExchange(
    val ctaToken: String,
    val country: Country,
    val testKit: TestKit,
    val mobileOS: MobileOS,
    val tokenAgeRange: TokenAgeRange,
    val appVersion: MobileAppVersion
) : Event(Info)

data class CtaExchangeCompleted(val ctaToken: CtaToken): Event(Info)
