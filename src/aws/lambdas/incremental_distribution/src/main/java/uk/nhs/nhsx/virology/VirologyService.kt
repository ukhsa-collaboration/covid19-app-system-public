package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.exceptions.ApiResponseException
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.virology.VirologyPolicyConfig.VirologyCriteria.Companion.of
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequest
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponse
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequest
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponse
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.order.VirologyRequestType
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.TestResult
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.VirologyLookupResult
import uk.nhs.nhsx.virology.result.VirologyResultRequest
import uk.nhs.nhsx.virology.result.VirologyResultRequest.NonPositive
import uk.nhs.nhsx.virology.result.VirologyResultValidator
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Instant
import java.time.Period
import java.util.function.Supplier

class VirologyService(
    private val persistenceService: VirologyPersistenceService,
    private val tokensGenerator: TokensGenerator,
    private val systemClock: Supplier<Instant>,
    private val virologyPolicyConfig: VirologyPolicyConfig,
    private val events: Events
) {
    fun handleTestOrderRequest(
        websiteConfig: VirologyWebsiteConfig,
        virologyRequestType: VirologyRequestType
    ): VirologyOrderResponse {
        val testOrder = orderTest()
        val websiteUrl = websiteUrl(websiteConfig, testOrder, virologyRequestType)
        return VirologyOrderResponse(websiteUrl, testOrder)
    }

    private fun orderTest(): TestOrder {
        val expireAt = systemClock.get().plus(Period.ofWeeks(4)).epochSecond
        return persistenceService.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)
    }

    private fun websiteUrl(
        virologyWebsiteConfig: VirologyWebsiteConfig,
        testOrder: TestOrder,
        virologyRequestType: VirologyRequestType
    ): String {
        val ctaTokenParam = "?ctaToken=" + testOrder.ctaToken
        return if (virologyRequestType == VirologyRequestType.ORDER) virologyWebsiteConfig.orderWebsite + ctaTokenParam else virologyWebsiteConfig.registerWebsite + ctaTokenParam
    }

    fun virologyLookupForV1(virologyLookupRequest: VirologyLookupRequest): VirologyLookupResult {
        return persistenceService
            .getTestResult(virologyLookupRequest.testResultPollingToken)
            .map { testResult: TestResult -> virologyLookupResultV1(testResult) }
            .orElseGet { VirologyLookupResult.NotFound() }
    }

    private fun virologyLookupResultV1(testResult: TestResult): VirologyLookupResult {
        if (virologyPolicyConfig.shouldBlockV1TestResultQueries(testResult.testKit)) {
            return VirologyLookupResult.Pending()
        }
        if (testResult.isAvailable) {
            markTestDataForDeletion(testResult, VirologyDataTimeToLiveCalculator.DEFAULT_TTL.apply(systemClock))
            return VirologyLookupResult.Available(
                VirologyLookupResponse(
                    testResult.testEndDate,
                    testResult.testResult,
                    testResult.testKit
                )
            )
        }
        return VirologyLookupResult.Pending()
    }

    fun virologyLookupForV2(
        request: VirologyLookupRequestV2,
        mobileAppVersion: MobileAppVersion
    ): VirologyLookupResult {
        return persistenceService
            .getTestResult(request.testResultPollingToken)
            .map { virologyLookupResultV2(it, request.country, mobileAppVersion) }
            .orElseGet { VirologyLookupResult.NotFound() }
    }

    private fun virologyLookupResultV2(
        testResult: TestResult,
        country: Country,
        mobileAppVersion: MobileAppVersion
    ): VirologyLookupResult {
        val virologyCriteria = of(country, testResult.testKit, testResult.testResult)
        if (virologyPolicyConfig.shouldBlockV2TestResultQueries(virologyCriteria, mobileAppVersion)) {
            return VirologyLookupResult.Pending()
        }
        if (testResult.isAvailable) {
            markTestDataForDeletion(testResult, VirologyDataTimeToLiveCalculator.DEFAULT_TTL.apply(systemClock))
            return VirologyLookupResult.AvailableV2(
                VirologyLookupResponseV2(
                    testResult.testEndDate,
                    testResult.testResult,
                    testResult.testKit,
                    virologyPolicyConfig.isDiagnosisKeysSubmissionSupported(virologyCriteria),
                    virologyPolicyConfig.isConfirmatoryTestRequired(virologyCriteria)
                )
            )
        }
        return VirologyLookupResult.Pending()
    }

    private fun markTestDataForDeletion(testResult: TestResult, virologyDataTimeToLive: VirologyDataTimeToLive) {
        try {
            persistenceService.markForDeletion(testResult, virologyDataTimeToLive)
        } catch (e: TransactionException) {
            events.emit(
                javaClass,
                TestResultMarkForDeletionFailure(
                    TestResultPollingToken.of(testResult.testResultPollingToken),
                    e.message!!
                )
            )
        }
    }

    fun acceptTestResult(testResult: VirologyResultRequest): VirologyResultPersistOperation {
        VirologyResultValidator.validateTestResult(testResult.testResult, testResult.testEndDate)
        return when (testResult.testResult) {
            VirologyResultRequest.NPEX_POSITIVE -> {
                val expireAt = systemClock.get().plus(Period.ofWeeks(4)).epochSecond
                val positive = VirologyResultRequest.Positive.from(testResult)
                persistenceService.persistPositiveTestResult(positive, expireAt)
            }
            VirologyResultRequest.NPEX_NEGATIVE, VirologyResultRequest.NPEX_VOID -> {
                val nonPositive = NonPositive.from(testResult)
                persistenceService.persistNonPositiveTestResult(nonPositive)
            }
            else -> throw ApiResponseException(
                HttpStatusCode.UNPROCESSABLE_ENTITY_422,
                "validation error: Invalid test result value"
            )
        }
    }

    fun acceptTestResultGeneratingTokens(tokenGenRequest: VirologyTokenGenRequest): VirologyTokenGenResponse {
        VirologyResultValidator.validateTestResult(tokenGenRequest.testResult, tokenGenRequest.testEndDate)
        val expireAt = systemClock.get().plus(Period.ofWeeks(4)).epochSecond
        val testOrder = persistenceService.persistTestOrderAndResult(
            { tokensGenerator.generateVirologyTokens() },
            expireAt,
            tokenGenRequest.testResult,
            tokenGenRequest.testEndDate,
            tokenGenRequest.testKit
        )

        events.emit(javaClass, InfoEvent("Token gen created ctaToken: ${testOrder.ctaToken.value}"))
        return VirologyTokenGenResponse.of(testOrder.ctaToken.value)
    }

    fun exchangeCtaTokenForV1(request: CtaExchangeRequest): CtaExchangeResult {
        return persistenceService
            .getTestOrder(request.ctaToken)
            .filter { it.downloadCounter < MAX_CTA_EXCHANGE_RETRY_COUNT }
            .map { testOrder: TestOrder ->
                persistenceService
                    .getTestResult(testOrder.testResultPollingToken)
                    .map { testResult: TestResult -> mapToCtaExchangeResultV1(testOrder, testResult) }
                    .orElseGet {
                        ctaTokenNotFound(
                            testOrder.ctaToken,
                            testOrder.testResultPollingToken,
                            testOrder.diagnosisKeySubmissionToken
                        )
                    }
            }
            .orElseGet { ctaTokenNotFound(request.ctaToken) }
    }

    private fun mapToCtaExchangeResultV1(testOrder: TestOrder, testResult: TestResult): CtaExchangeResult {
        return when {
            virologyPolicyConfig.shouldBlockV1TestResultQueries(testResult.testKit) ->
                ctaTokenNotFound(
                    testOrder.ctaToken,
                    testOrder.testResultPollingToken,
                    testOrder.diagnosisKeySubmissionToken
                )
            testResult.isAvailable -> {
                persistenceService.updateOnCtaExchange(
                    testOrder, testResult, VirologyDataTimeToLiveCalculator.DEFAULT_TTL.apply(
                        systemClock
                    )
                )
                val response = CtaExchangeResponse(
                    testOrder.diagnosisKeySubmissionToken.value,
                    testResult.testResult,
                    testResult.testEndDate,
                    testResult.testKit
                )
                events.emit(
                    javaClass,
                    InfoEvent("Cta token exchange successful for ctaToken: ${testOrder.ctaToken.value}")
                )
                CtaExchangeResult.Available(response)
            }
            else -> {
                events.emit(javaClass, InfoEvent("Cta token exchange: virology result not available yet"))
                CtaExchangeResult.Pending()
            }
        }
    }

    fun exchangeCtaTokenForV2(
        request: CtaExchangeRequestV2,
        mobileAppVersion: MobileAppVersion
    ): CtaExchangeResult {
        return persistenceService
            .getTestOrder(request.ctaToken)
            .filter { it.downloadCounter < MAX_CTA_EXCHANGE_RETRY_COUNT }
            .map { testOrder: TestOrder ->
                persistenceService
                    .getTestResult(testOrder.testResultPollingToken)
                    .map { testResult: TestResult ->
                        mapToCtaExchangeResultV2(
                            testOrder,
                            testResult,
                            request.country,
                            mobileAppVersion
                        )
                    }
                    .orElseGet {
                        ctaTokenNotFound(
                            testOrder.ctaToken,
                            testOrder.testResultPollingToken,
                            testOrder.diagnosisKeySubmissionToken
                        )
                    }
            }
            .orElseGet { ctaTokenNotFound(request.ctaToken) }
    }

    private fun mapToCtaExchangeResultV2(
        testOrder: TestOrder,
        testResult: TestResult,
        country: Country,
        mobileAppVersion: MobileAppVersion
    ): CtaExchangeResult {
        val virologyCriteria = of(country, testResult.testKit, testResult.testResult)
        return when {
            virologyPolicyConfig.shouldBlockV2TestResultQueries(virologyCriteria, mobileAppVersion) -> {
                ctaTokenNotFound(
                    testOrder.ctaToken,
                    testOrder.testResultPollingToken,
                    testOrder.diagnosisKeySubmissionToken
                )
            }
            testResult.isAvailable -> {
                persistenceService.updateOnCtaExchange(
                    testOrder, testResult, VirologyDataTimeToLiveCalculator.DEFAULT_TTL.apply(
                        systemClock
                    )
                )
                val response = CtaExchangeResponseV2(
                    testOrder.diagnosisKeySubmissionToken.value,
                    testResult.testResult,
                    testResult.testEndDate,
                    testResult.testKit,
                    virologyPolicyConfig.isDiagnosisKeysSubmissionSupported(virologyCriteria),
                    virologyPolicyConfig.isConfirmatoryTestRequired(virologyCriteria)
                )
                events.emit(
                    javaClass,
                    InfoEvent("Cta token exchange successful for ctaToken: ${testOrder.ctaToken.value}")
                )
                CtaExchangeResult.AvailableV2(response)
            }
            else -> CtaExchangeResult.Pending()
        }
    }

    private fun ctaTokenNotFound(
        ctaToken: CtaToken? = null,
        testResultPollingToken: TestResultPollingToken? = null,
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken? = null
    ): CtaExchangeResult.NotFound {
        events.emit(javaClass, CtaTokenNotFound(ctaToken, testResultPollingToken, diagnosisKeySubmissionToken))
        return CtaExchangeResult.NotFound()
    }

    private val MAX_CTA_EXCHANGE_RETRY_COUNT = 2
}
