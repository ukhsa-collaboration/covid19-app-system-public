@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.virology

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.DownloadCountExceeded
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.PolicyRejectionV1
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.TestOrderNotFound
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.TestResultNotFound
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.AvailableV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Pending
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.order.VirologyRequestType.ORDER
import uk.nhs.nhsx.virology.order.VirologyRequestType.REGISTER
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.Success
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period

class VirologyServiceV1Test {

    private val events = RecordingEvents()
    private val now = Instant.EPOCH
    private val clock = { now }

    private val fourWeeksExpireAt = now.plus(Period.ofWeeks(4))
    private val mobileOS = MobileOS.iOS
    private val mobileAppVersion = MobileAppVersion.Version(5, 0)
    private val websiteConfig = VirologyWebsiteConfig(
        orderWebsite = "https://example.order-a-test.uk",
        registerWebsite = "https://example.register-a-test.uk"
    )

    private val tokenGenerator = mockk<TokensGenerator>()
    private val persistenceService = mockk<VirologyPersistenceService>()
    private val virologyPolicyConfig = mockk<VirologyPolicyConfig>()

    @Test
    fun `order virology test`() {
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            CtaToken.of("cc8f0b6z"),
            TestResultPollingToken.of("polling-token"),
            DiagnosisKeySubmissionToken.of("submission-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val service = VirologyService(tokenGenerator)
        val response = service.handleTestOrderRequest(websiteConfig, ORDER)

        expectThat(response) {
            get(VirologyOrderResponse::diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("submission-token"))
            get(VirologyOrderResponse::testResultPollingToken).isEqualTo(TestResultPollingToken.of("polling-token"))
            get(VirologyOrderResponse::tokenParameterValue).isEqualTo(CtaToken.of("cc8f0b6z"))
            get(VirologyOrderResponse::websiteUrlWithQuery).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")
        }

        verifySequence {
            persistenceService.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `register for virology test`() {
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            CtaToken.of("cc8f0b6z"),
            TestResultPollingToken.of("polling-token"),
            DiagnosisKeySubmissionToken.of("submission-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val service = VirologyService(tokenGenerator)
        val response = service.handleTestOrderRequest(websiteConfig, REGISTER)

        expectThat(response) {
            get(VirologyOrderResponse::diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("submission-token"))
            get(VirologyOrderResponse::testResultPollingToken).isEqualTo(TestResultPollingToken.of("polling-token"))
            get(VirologyOrderResponse::tokenParameterValue).isEqualTo(CtaToken.of("cc8f0b6z"))
            get(VirologyOrderResponse::websiteUrlWithQuery).isEqualTo("https://example.register-a-test.uk?ctaToken=cc8f0b6z")
        }

        verify(exactly = 1) {
            persistenceService.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `exchanges cta token`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistenceService.getTestOrder(ctaToken) } returns testOrder
        every { persistenceService.getTestResult(testResultPollingToken) } returns testResult
        every { persistenceService.updateOnCtaExchange(any(), any(), any()) } just runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val result = VirologyService().exchangeCtaTokenForV1(
            CtaExchangeRequestV1(ctaToken),
            mobileOS,
            mobileAppVersion
        )

        expectThat(result)
            .isA<AvailableV1>()
            .get(AvailableV1::ctaExchangeResponse).and {
                get(CtaExchangeResponseV1::diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("sub-token"))
                get(CtaExchangeResponseV1::testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
                get(CtaExchangeResponseV1::testResult).isEqualTo(Positive)
            }

        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
            persistenceService.updateOnCtaExchange(testOrder, testResult, any())
        }

        expectThat(events).contains(SuccessfulCtaExchange::class, CtaExchangeCompleted::class)
    }

    @Test
    fun `exchanges cta token without test result`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.pendingTestResult

        every { persistenceService.getTestOrder(ctaToken) } returns testOrder
        every { persistenceService.getTestResult(testResultPollingToken) } returns testResult
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val result = VirologyService().exchangeCtaTokenForV1(
            CtaExchangeRequestV1(ctaToken),
            mobileOS,
            mobileAppVersion
        )

        expectThat(result).isA<Pending>()

        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
        }

        expectThat(events).contains(VirologyResultPending::class)
    }

    @Test
    fun `exchanges cta token and does not find match`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        every { persistenceService.getTestOrder(ctaToken) } returns null

        val result = VirologyService().exchangeCtaTokenForV1(
            CtaExchangeRequestV1(ctaToken),
            mobileOS,
            mobileAppVersion
        )

        expectThat(result).isA<NotFound>()

        verifySequence { persistenceService.getTestOrder(ctaToken) }

        expectThat(events).contains(TestOrderNotFound::class)
    }

    @Test
    fun `exchanges cta token and does not find test result match`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistenceService.getTestOrder(ctaToken) } returns testOrder
        every { persistenceService.getTestResult(testResultPollingToken) } returns null

        val result = VirologyService().exchangeCtaTokenForV1(
            CtaExchangeRequestV1(ctaToken),
            mobileOS,
            mobileAppVersion
        )

        expectThat(result).isA<NotFound>()

        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
        }

        expectThat(events).contains(TestResultNotFound::class)
    }

    @Test
    fun `exchanges cta token for rapid result returns not found`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistenceService.getTestOrder(ctaToken) } returns testOrder
        every { persistenceService.getTestResult(testResultPollingToken) } returns TestData.positiveRapidResult
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns true

        val result = VirologyService().exchangeCtaTokenForV1(
            CtaExchangeRequestV1(ctaToken),
            mobileOS,
            mobileAppVersion
        )

        expectThat(result).isA<NotFound>()

        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
        }

        expectThat(events).contains(PolicyRejectionV1::class)
    }

    @Test
    fun `when download counter is 2, return not found`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            2,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistenceService.getTestOrder(ctaToken) } returns testOrder
        every { persistenceService.getTestResult(testResultPollingToken) } returns testResult
        every { persistenceService.markForDeletion(any(), any()) } just runs

        val result = VirologyService().exchangeCtaTokenForV1(
            CtaExchangeRequestV1(ctaToken),
            mobileOS,
            mobileAppVersion
        )

        expectThat(result).isA<NotFound>()

        verify(exactly = 1) { persistenceService.getTestOrder(ctaToken) }

        expectThat(events).contains(DownloadCountExceeded::class)
    }

    @Test
    fun `persists result correctly for positive result`() {
        every {
            persistenceService.persistTestResultWithKeySubmission(
                testResult = any(),
                expireAt = any()
            )
        } returns Success()

        val testResult = NPEXTestResultWith(Positive)

        VirologyService().acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistTestResultWithKeySubmission(testResult, fourWeeksExpireAt)
        }
    }

    @Test
    fun `persists result correctly for negative result`() {
        every { persistenceService.persistTestResultWithoutKeySubmission(any()) } returns Success()

        val testResult = NPEXTestResultWith(Negative)

        VirologyService().acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistTestResultWithoutKeySubmission(testResult)
        }
    }

    @Test
    fun `persists result correctly for void result`() {
        every { persistenceService.persistTestResultWithoutKeySubmission(any()) } returns Success()

        val testResult = NPEXTestResultWith(Void)

        VirologyService().acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistTestResultWithoutKeySubmission(testResult)
        }
    }

    @Test
    fun `accepts test lab virology positive result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("074qbxqq"),
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"),
            LocalDateTime.now().plusWeeks(4)
        )

        every {
            persistenceService.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = any(),
                testResult = any(),
                testEndDate = any(),
                testKit = any()
            )
        } returns testOrderTokens

        val response = VirologyService().acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Positive,
                LAB_RESULT
            )
        )

        expectThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("074qbxqq")))

        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = fourWeeksExpireAt,
                testResult = Positive,
                testEndDate = TestEndDate.of(2020, 8, 7),
                testKit = any()
            )
        }
    }

    @Test
    fun `accepts test lab virology negative result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("1e19z5zt"),
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"),
            LocalDateTime.now().plusWeeks(4)
        )

        every {
            persistenceService.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = any(),
                testResult = any(),
                testEndDate = any(),
                testKit = any()
            )
        } returns testOrderTokens

        val response = VirologyService().acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                testEndDate = TestEndDate.of(2020, 6, 7),
                testResult = Negative,
                testKit = LAB_RESULT
            )
        )

        expectThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))

        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = fourWeeksExpireAt,
                testResult = Negative,
                testEndDate = TestEndDate.of(2020, 6, 7),
                testKit = any()
            )
        }
    }

    @Test
    fun `accepts test lab virology void result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("1e19z5zt"),
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"),
            LocalDateTime.now().plusWeeks(4)
        )

        every {
            persistenceService.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = any(),
                testResult = any(),
                testEndDate = any(),
                testKit = any()
            )
        } returns testOrderTokens

        val response = VirologyService().acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                testEndDate = TestEndDate.of(2020, 8, 7),
                testResult = Void,
                testKit = LAB_RESULT
            )
        )

        expectThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))

        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = fourWeeksExpireAt,
                testResult = Void,
                testEndDate = TestEndDate.of(2020, 8, 7),
                testKit = any()
            )
        }
    }

    @Test
    fun `exchanges cta token v1 with lfd test type is found`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.positiveRapidResult

        every { persistenceService.getTestOrder(ctaToken) } returns testOrder
        every { persistenceService.getTestResult(testResultPollingToken) } returns testResult
        every { persistenceService.updateOnCtaExchange(any(), any(), any()) } just runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val result = VirologyService().exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken), mobileOS, mobileAppVersion)

        expectThat(result).isA<AvailableV1>()

        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
            persistenceService.updateOnCtaExchange(testOrder, testResult, any())
        }
    }

    private fun NPEXTestResultWith(testResult: TestResult) = VirologyResultRequestV2(
        ctaToken = CtaToken.of("cc8f0b6z"),
        testEndDate = TestEndDate.of(2020, 4, 23),
        testResult = testResult,
        testKit = LAB_RESULT
    )

    private fun VirologyService(tokenGenerator: TokensGenerator = TokensGenerator) = VirologyService(
        persistence = persistenceService,
        tokensGenerator = tokenGenerator,
        clock = clock,
        policyConfig = virologyPolicyConfig,
        events = events
    )
}
