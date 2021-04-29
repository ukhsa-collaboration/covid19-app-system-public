package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestResult.*
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.virology.VirologyPolicyConfig.VirologyCriteria
import uk.nhs.nhsx.virology.exchange.*
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.order.VirologyRequestType
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.TestState
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator.Companion.DEFAULT_TTL
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Period

class VirologyService(
    private val persistence: VirologyPersistenceService,
    private val tokensGenerator: TokensGenerator,
    private val clock: Clock,
    private val policyConfig: VirologyPolicyConfig,
    private val events: Events
) {
    private val maxCtaExchangeRetryCount = 2

    fun handleTestOrderRequest(
        websiteConfig: VirologyWebsiteConfig,
        requestType: VirologyRequestType
    ): VirologyOrderResponse {
        val testOrder = orderTest()
        val websiteUrl = websiteUrl(websiteConfig, testOrder, requestType)
        return VirologyOrderResponse(websiteUrl, testOrder)
    }

    private fun orderTest(): TestOrder {
        val expireAt = clock().plus(Period.ofWeeks(4))
        return persistence.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)
    }

    private fun websiteUrl(
        websiteConfig: VirologyWebsiteConfig,
        testOrder: TestOrder,
        requestType: VirologyRequestType
    ): String {
        val ctaTokenParam = "?ctaToken=" + testOrder.ctaToken
        return if (requestType == VirologyRequestType.ORDER) websiteConfig.orderWebsite + ctaTokenParam else websiteConfig.registerWebsite + ctaTokenParam
    }

    fun acceptTestResult(testResult: VirologyResultRequestV2): VirologyResultPersistOperation {
        return when (testResult.testResult) {
            Positive -> persistence.persistPositiveTestResult(testResult, clock().plus(Period.ofWeeks(4)))
            Negative, Void -> persistence.persistNonPositiveTestResult(testResult)
        }
    }

    fun acceptTestResultGeneratingTokens(tokenGenRequest: VirologyTokenGenRequestV2): VirologyTokenGenResponse {
        val expireAt = clock().plus(Period.ofWeeks(4))
        val testOrder = persistence.persistTestOrderAndResult(
            { tokensGenerator.generateVirologyTokens() },
            expireAt,
            tokenGenRequest.testResult,
            tokenGenRequest.testEndDate,
            tokenGenRequest.testKit
        )

        events(InfoEvent("Token gen created ctaToken: ${testOrder.ctaToken.value}"))
        return VirologyTokenGenResponse(testOrder.ctaToken)
    }

    fun exchangeCtaTokenForV1(request: CtaExchangeRequestV1): CtaExchangeResult {
        return persistence
            .getTestOrder(request.ctaToken)
            .filter { it.downloadCounter < maxCtaExchangeRetryCount }
            .map { testOrder: TestOrder ->
                persistence
                    .getTestResult(testOrder.testResultPollingToken)
                    .map { mapToCtaExchangeResultV1(testOrder, it) }
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

    private fun mapToCtaExchangeResultV1(testOrder: TestOrder, testState: TestState): CtaExchangeResult {
        return when {
            policyConfig.shouldBlockV1TestResultQueries(testState.testKit) ->
                ctaTokenNotFound(
                    testOrder.ctaToken,
                    testOrder.testResultPollingToken,
                    testOrder.diagnosisKeySubmissionToken
                )
            testState is AvailableTestResult -> {
                persistence.updateOnCtaExchange(
                    testOrder,
                    testState,
                    DEFAULT_TTL(clock)
                )
                val response = CtaExchangeResponseV1(
                    testOrder.diagnosisKeySubmissionToken,
                    testState.testResult,
                    testState.testEndDate,
                    testState.testKit
                )
                events(
                    InfoEvent("Cta token exchange successful for ctaToken: ${testOrder.ctaToken.value}")
                )
                CtaExchangeResult.Available(response)
            }
            else -> {
                events(InfoEvent("Cta token exchange: virology result not available yet"))
                CtaExchangeResult.Pending()
            }
        }
    }

    fun exchangeCtaTokenForV2(
        request: CtaExchangeRequestV2,
        mobileAppVersion: MobileAppVersion
    ): CtaExchangeResult {
        return persistence
            .getTestOrder(request.ctaToken)
            .filter { it.downloadCounter < maxCtaExchangeRetryCount }
            .map { testOrder: TestOrder ->
                persistence
                    .getTestResult(testOrder.testResultPollingToken)
                    .map {
                        mapToCtaExchangeResultV2(
                            testOrder,
                            it,
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
        testState: TestState,
        country: Country,
        mobileAppVersion: MobileAppVersion
    ): CtaExchangeResult {
        return when (testState) {
            is PendingTestResult -> CtaExchangeResult.Pending()
            is AvailableTestResult -> {
                val virologyCriteria = VirologyCriteria(country, testState.testKit, testState.testResult)
                when {
                    policyConfig.shouldBlockV2TestResultQueries(virologyCriteria, mobileAppVersion) -> {
                        ctaTokenNotFound(
                            testOrder.ctaToken,
                            testOrder.testResultPollingToken,
                            testOrder.diagnosisKeySubmissionToken
                        )
                    }
                    else -> {
                        persistence.updateOnCtaExchange(
                            testOrder, testState, DEFAULT_TTL(clock)
                        )
                        val response = CtaExchangeResponseV2(
                            testOrder.diagnosisKeySubmissionToken,
                            testState.testResult,
                            testState.testEndDate,
                            testState.testKit,
                            policyConfig.isDiagnosisKeysSubmissionSupported(virologyCriteria),
                            policyConfig.isConfirmatoryTestRequired(virologyCriteria)
                        )
                        events(
                            InfoEvent("Cta token exchange successful for ctaToken: ${testOrder.ctaToken.value}")
                        )
                        CtaExchangeResult.AvailableV2(response)
                    }
                }
            }
        }
    }

    private fun ctaTokenNotFound(
        ctaToken: CtaToken? = null,
        testResultPollingToken: TestResultPollingToken? = null,
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken? = null
    ): CtaExchangeResult.NotFound {
        events(CtaTokenNotFound(ctaToken, testResultPollingToken, diagnosisKeySubmissionToken))
        return CtaExchangeResult.NotFound()
    }
}
