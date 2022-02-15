package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator.Companion.checksum
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestJourney.CtaExchange
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Plod
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void
import uk.nhs.nhsx.domain.TokenAgeRange
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.DownloadCountExceeded
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.PolicyRejectionV1
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.PolicyRejectionV2
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.TestOrderNotFound
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.TestResultNotFound
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.AvailableV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.AvailableV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Pending
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.order.VirologyRequestType
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.TestOrderExpiryCalculator
import uk.nhs.nhsx.virology.persistence.TestOrderExpiryCalculator.DEFAULT_EXPIRY
import uk.nhs.nhsx.virology.persistence.TestState
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator.Companion.DEFAULT_TTL
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.policy.VirologyCriteria
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import uk.nhs.nhsx.virology.result.VirologyTokenStatusRequest
import uk.nhs.nhsx.virology.result.VirologyTokenStatusResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

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

    private fun orderTest() = persistence.persistTestOrder(
        tokensGenerator::generateVirologyTokens,
        TestOrderExpiryCalculator(clock)
    )

    private fun websiteUrl(
        websiteConfig: VirologyWebsiteConfig,
        testOrder: TestOrder,
        requestType: VirologyRequestType
    ): String {
        val ctaTokenParam = "?ctaToken=" + testOrder.ctaToken
        return if (requestType == VirologyRequestType.ORDER) websiteConfig.orderWebsite + ctaTokenParam else websiteConfig.registerWebsite + ctaTokenParam
    }

    fun acceptTestResult(testResult: VirologyResultRequestV2) = when (testResult.testResult) {
        Positive -> persistence.persistTestResultWithKeySubmission(testResult, TestOrderExpiryCalculator(clock))
        Negative, Void, Plod -> persistence.persistTestResultWithoutKeySubmission(testResult)
    }

    fun acceptTestResultGeneratingTokens(tokenGenRequest: VirologyTokenGenRequestV2): VirologyTokenGenResponse {
        val testOrder = persistence.persistTestOrderAndResult(
            tokensGenerator::generateVirologyTokens,
            TestOrderExpiryCalculator(clock),
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
    ) = if (!(checksum().validate(tokenStatusRequest.ctaToken))) {
        VirologyTokenStatusResponse("other")
    } else persistence.getTestOrder(CtaToken.of(tokenStatusRequest.ctaToken))
        ?.takeIf { it.downloadCounter == 0 }
        ?.let {
            persistence.getTestResult(it.testResultPollingToken)
                ?.apply { events(ConsumableTokenStatusCheck(2, source, testKit)) }
            VirologyTokenStatusResponse("consumable")
        }
        ?: VirologyTokenStatusResponse("other")

    fun exchangeCtaTokenForV1(
        request: CtaExchangeRequestV1,
        mobileOS: MobileOS,
        mobileAppVersion: MobileAppVersion
    ) = getTestResults(request.ctaToken)
        ?.let { (testOrder, testState) ->
            mapToCtaExchangeResultV1(
                testOrder = testOrder,
                testState = testState,
                mobileOS = mobileOS,
                mobileAppVersion = mobileAppVersion
            )
        } ?: NotFound(request.ctaToken)

    fun exchangeCtaTokenForV2(
        request: CtaExchangeRequestV2,
        mobileAppVersion: MobileAppVersion,
        mobileOS: MobileOS
    ) = getTestResults(request.ctaToken)
        ?.let { (testOrder, testState) ->
            mapToCtaExchangeResultV2(
                testOrder = testOrder,
                testState = testState,
                country = request.country,
                appVersion = mobileAppVersion,
                os = mobileOS
            )
        } ?: NotFound(request.ctaToken)

    private fun getTestResults(ctaToken: CtaToken): Pair<TestOrder, TestState>? {
        val testOrder = persistence.getTestOrder(ctaToken)
        if (testOrder == null) {
            events(TestOrderNotFound(ctaToken))
            return null
        }

        if (testOrder.downloadCounter >= maxCtaExchangeRetryCount) {
            events(DownloadCountExceeded(testOrder.ctaToken, testOrder.downloadCounter))
            return null
        }

        val testState = persistence.getTestResult(testOrder.testResultPollingToken)
        if (testState == null) {
            events(TestResultNotFound(testOrder.ctaToken, testOrder.testResultPollingToken))
            return null
        }

        return testOrder to testState
    }

    private fun mapToCtaExchangeResultV1(
        testOrder: TestOrder,
        testState: TestState,
        mobileOS: MobileOS,
        mobileAppVersion: MobileAppVersion,
    ): CtaExchangeResult {
        if (policyConfig.shouldBlockV1TestResultQueries(testState.testKit)) {
            events(PolicyRejectionV1(testOrder.ctaToken, testState.testKit))
            return NotFound(testOrder.ctaToken)
        }

        return when (testState) {
            is AvailableTestResult -> {
                val tokenAgeRange = testOrder.getTokenAgeRange()
                persistence.updateOnCtaExchange(testOrder, testState, DEFAULT_TTL(clock))

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
                events(InfoEvent("Cta token exchange successful for ctaToken: ${testOrder.ctaToken.value}"))
                events(CtaExchangeCompleted(testOrder.ctaToken))
                AvailableV1(response)
            }
            is PendingTestResult -> {
                events(VirologyResultPending(testOrder.ctaToken))
                Pending
            }
        }
    }

    private fun mapToCtaExchangeResultV2(
        testOrder: TestOrder,
        testState: TestState,
        country: Country,
        appVersion: MobileAppVersion,
        os: MobileOS
    ) = when (testState) {
        is AvailableTestResult -> {
            val virologyCriteria = VirologyCriteria(CtaExchange, country, testState.testKit, testState.testResult)
            when {
                policyConfig.shouldBlockV2TestResultQueries(virologyCriteria, appVersion) -> {
                    events(PolicyRejectionV2(testOrder.ctaToken, testState.testKit, country, appVersion, os))
                    NotFound(testOrder.ctaToken)
                }
                else -> {
                    persistence.updateOnCtaExchange(testOrder, testState, DEFAULT_TTL(clock))
                    val response = CtaExchangeResponseV2(
                        diagnosisKeySubmissionToken = testOrder.diagnosisKeySubmissionToken,
                        testResult = testState.testResult,
                        testEndDate = testState.testEndDate,
                        testKit = testState.testKit,
                        diagnosisKeySubmissionSupported = policyConfig.isDiagnosisKeysSubmissionSupported(virologyCriteria),
                        requiresConfirmatoryTest = policyConfig.isConfirmatoryTestRequired(virologyCriteria, appVersion),
                        confirmatoryDayLimit = policyConfig.confirmatoryDayLimit(virologyCriteria, appVersion),
                        shouldOfferFollowUpTest = policyConfig.shouldOfferFollowUpTest(virologyCriteria, appVersion)
                    )

                    if (testOrder.downloadCounter == 0 && testState.isPositive()) {
                        val tokenAgeRange = testOrder.getTokenAgeRange()
                        events(
                            SuccessfulCtaExchange(
                                testOrder.ctaToken.value,
                                country,
                                testState.testKit,
                                os,
                                tokenAgeRange,
                                appVersion
                            )
                        )
                    }
                    events(InfoEvent("Cta token exchange successful for ctaToken: ${testOrder.ctaToken.value}"))
                    events(CtaExchangeCompleted(testOrder.ctaToken))
                    AvailableV2(response)
                }
            }
        }
        is PendingTestResult -> {
            events(VirologyResultPending(testOrder.ctaToken))
            Pending
        }
    }

    private fun TestOrder.getTokenAgeRange(): TokenAgeRange {
        val initialDateTimeOfEntry = expiryTimeToLive.minus(DEFAULT_EXPIRY)
        val currentTime = LocalDateTime.ofInstant(clock(), UTC)
        val timeElapsed = Duration.between(initialDateTimeOfEntry, currentTime)
        return TokenAgeRange.from(timeElapsed)
    }
}
