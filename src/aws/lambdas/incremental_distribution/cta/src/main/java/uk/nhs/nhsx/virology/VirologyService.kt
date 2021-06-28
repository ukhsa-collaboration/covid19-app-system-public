package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.SuccessfulCtaExchange
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator.Companion.checksum
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestJourney.CtaExchange
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Plod
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.domain.TokenAgeRange
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
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
import uk.nhs.nhsx.virology.policy.VirologyCriteria
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import uk.nhs.nhsx.virology.result.VirologyTokenStatusRequest
import uk.nhs.nhsx.virology.result.VirologyTokenStatusResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset

class VirologyService(
    private val persistence: VirologyPersistenceService,
    private val tokensGenerator: TokensGenerator,
    private val clock: Clock,
    private val policyConfig: VirologyPolicyConfig,
    private val events: Events
) {
    private val maxCtaExchangeRetryCount = 2
    private val expireAtWeeks = 4

    fun handleTestOrderRequest(
        websiteConfig: VirologyWebsiteConfig,
        requestType: VirologyRequestType
    ): VirologyOrderResponse {
        val testOrder = orderTest()
        val websiteUrl = websiteUrl(websiteConfig, testOrder, requestType)
        return VirologyOrderResponse(websiteUrl, testOrder)
    }

    private fun orderTest(): TestOrder {
        val expireAt = clock().plus(Period.ofWeeks(expireAtWeeks))
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

    fun acceptTestResult(testResult: VirologyResultRequestV2): VirologyResultPersistOperation =
        when (testResult.testResult) {
            Positive -> persistence.persistTestResultWithKeySubmission(testResult, clock().plus(Period.ofWeeks(4)))
            Negative, Void, Plod -> persistence.persistTestResultWithoutKeySubmission(testResult)
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

    fun checkStatusOfToken(
        tokenStatusRequest: VirologyTokenStatusRequest,
        source: VirologyTokenExchangeSource
    ): VirologyTokenStatusResponse {
        if (!(checksum().validate(tokenStatusRequest.ctaToken))) {
            return VirologyTokenStatusResponse("other")
        }
        val testOrder = persistence.getTestOrder(CtaToken.of(tokenStatusRequest.ctaToken))

        return testOrder.filter { it.downloadCounter == 0 }
            .map {
                val testResult = persistence.getTestResult(it.testResultPollingToken)
                events(ConsumableTokenStatusCheck(2, source, testResult.get().testKit))
                VirologyTokenStatusResponse("consumable")
            }.orElse(VirologyTokenStatusResponse("other"))
    }

    fun exchangeCtaTokenForV1(request: CtaExchangeRequestV1, mobileOS: MobileOS, mobileAppVersion: MobileAppVersion): CtaExchangeResult {
        return persistence
            .getTestOrder(request.ctaToken)
            .filter { it.downloadCounter < maxCtaExchangeRetryCount }
            .map { testOrder: TestOrder ->
                persistence
                    .getTestResult(testOrder.testResultPollingToken)
                    .map { mapToCtaExchangeResultV1(testOrder, it, mobileOS, mobileAppVersion) }
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

    private fun mapToCtaExchangeResultV1(
        testOrder: TestOrder,
        testState: TestState,
        mobileOS: MobileOS,
        mobileAppVersion: MobileAppVersion,
    ): CtaExchangeResult {
        return when {
            policyConfig.shouldBlockV1TestResultQueries(testState.testKit) ->
                ctaTokenNotFound(
                    testOrder.ctaToken,
                    testOrder.testResultPollingToken,
                    testOrder.diagnosisKeySubmissionToken
                )
            testState is AvailableTestResult -> {
                val tokenAgeRange = getTokenAgeRange(testOrder)
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
                if (testOrder.downloadCounter == 0 && testState.isPositive()) {
                    events(
                        SuccessfulCtaExchange(
                            testOrder.ctaToken.value,
                            Country.UNKNOWN,
                            testState.testKit,
                            mobileOS,
                            tokenAgeRange,
                            mobileAppVersion
                        )
                    )
                }
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
        mobileAppVersion: MobileAppVersion,
        mobileOS: MobileOS
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
                            mobileAppVersion,
                            mobileOS
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
        mobileAppVersion: MobileAppVersion,
        mobileOS: MobileOS
    ): CtaExchangeResult {
        return when (testState) {
            is PendingTestResult -> CtaExchangeResult.Pending()
            is AvailableTestResult -> {
                val virologyCriteria = VirologyCriteria(CtaExchange, country, testState.testKit, testState.testResult)

                when {
                    policyConfig.shouldBlockV2TestResultQueries(virologyCriteria, mobileAppVersion) -> {
                        ctaTokenNotFound(
                            testOrder.ctaToken,
                            testOrder.testResultPollingToken,
                            testOrder.diagnosisKeySubmissionToken
                        )
                    }
                    else -> {
                        persistence.updateOnCtaExchange(testOrder, testState, DEFAULT_TTL(clock))
                        val response = CtaExchangeResponseV2(
                            testOrder.diagnosisKeySubmissionToken,
                            testState.testResult,
                            testState.testEndDate,
                            testState.testKit,
                            policyConfig.isDiagnosisKeysSubmissionSupported(virologyCriteria),
                            policyConfig.isConfirmatoryTestRequired(virologyCriteria, mobileAppVersion),
                            policyConfig.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)
                        )

                        if (testOrder.downloadCounter == 0 && testState.isPositive()) {
                            val tokenAgeRange = getTokenAgeRange(testOrder)
                            events(
                                SuccessfulCtaExchange(
                                    testOrder.ctaToken.value,
                                    country,
                                    testState.testKit,
                                    mobileOS,
                                    tokenAgeRange,
                                    mobileAppVersion
                                )
                            )
                        }
                        events(InfoEvent("Cta token exchange successful for ctaToken: ${testOrder.ctaToken.value}"))
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

    private fun getTokenAgeRange(testOrder: TestOrder): TokenAgeRange {
        val initialDateTimeOfEntry = testOrder.expiryTimeToLive.minusWeeks(expireAtWeeks.toLong())
        val currentTime = LocalDateTime.ofInstant(clock(), ZoneOffset.UTC)
        val timeElapsed = Duration.between(initialDateTimeOfEntry, currentTime)
        return TokenAgeRange.from(timeElapsed)
    }
}
