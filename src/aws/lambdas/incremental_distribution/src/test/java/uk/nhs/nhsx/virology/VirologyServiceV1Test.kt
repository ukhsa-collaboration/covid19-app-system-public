package uk.nhs.nhsx.virology

import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.*
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult.*
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyRequestType
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Instant
import java.time.Period
import java.util.*

class VirologyServiceV1Test {

    private val events = RecordingEvents()
    private val now = Instant.EPOCH
    private val clock = { now }
    private val persistenceService = mockk<VirologyPersistenceService>()
    private val fourWeeksExpireAt = now.plus(Period.ofWeeks(4))
    private val threeWeeksExpireAt = now.plus(Period.ofWeeks(3))
    private val virologyPolicyConfig = mockk<VirologyPolicyConfig>()
    private val websiteConfig = VirologyWebsiteConfig(
        "https://example.order-a-test.uk",
        "https://example.register-a-test.uk"
    )

    @Test
    fun `order virology test`() {
        val tokenGenerator = mockk<TokensGenerator>()
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            CtaToken.of("cc8f0b6z"),
            TestResultPollingToken.of("polling-token"),
            DiagnosisKeySubmissionToken.of("submission-token")
        )

        val service = virologyService(tokenGenerator)

        val response = service.handleTestOrderRequest(websiteConfig, VirologyRequestType.ORDER)

        assertThat(response.diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("submission-token"))
        assertThat(response.testResultPollingToken).isEqualTo(TestResultPollingToken.of("polling-token"))
        assertThat(response.tokenParameterValue).isEqualTo(CtaToken.of("cc8f0b6z"))
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")

        verifySequence {
            persistenceService.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `register for virology test`() {
        val tokenGenerator = mockk<TokensGenerator>()
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            CtaToken.of("cc8f0b6z"),
            TestResultPollingToken.of("polling-token"),
            DiagnosisKeySubmissionToken.of("submission-token")
        )


        val service = virologyService(tokenGenerator)

        val response = service.handleTestOrderRequest(websiteConfig, VirologyRequestType.REGISTER)

        assertThat(response.diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("submission-token"))
        assertThat(response.testResultPollingToken).isEqualTo(TestResultPollingToken.of("polling-token"))
        assertThat(response.tokenParameterValue).isEqualTo(CtaToken.of("cc8f0b6z"))
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://example.register-a-test.uk?ctaToken=cc8f0b6z")

        verify(exactly = 1) {
            persistenceService.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `exchanges cta token`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistenceService.updateOnCtaExchange(any(), any(), any()) } just Runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = virologyService()
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken)) as CtaExchangeResult.Available

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("sub-token"))
        assertThat(result.ctaExchangeResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(result.ctaExchangeResponse.testResult).isEqualTo(Positive)

        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
            persistenceService.updateOnCtaExchange(testOrder, testResult, any())
        }
    }

    @Test
    fun `exchanges cta token without test result`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokens = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.pendingTestResult

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = virologyService()
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken))

        assertThat(result).isInstanceOf(CtaExchangeResult.Pending::class.java)
        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
        }
    }

    @Test
    fun `exchanges cta token and does not find match`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        every { persistenceService.getTestOrder(ctaToken) } returns Optional.empty()

        val service = virologyService()
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken))

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verifySequence { persistenceService.getTestOrder(ctaToken) }
    }

    @Test
    fun `exchanges cta token and does not find test result match`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.empty()

        val service = virologyService()
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken))

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
        }
    }

    @Test
    fun `exchanges cta token for rapid result returns not found`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokens = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(TestData.positiveRapidResult)
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns true

        val service = virologyService()
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken))

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
        }
    }

    @Test
    fun `when download counter is 2, return not found`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokensCounter =
            TestOrder(ctaToken, 2, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveResultFor(testTokensCounter.testResultPollingToken)

        every { persistenceService.getTestOrder(ctaToken) }.returns(
            (Optional.of(testTokensCounter))
        )
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = virologyService()

        assertThat(service.exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken))).isInstanceOf(CtaExchangeResult.NotFound::class.java)

        verify(exactly = 1) { persistenceService.getTestOrder(ctaToken) }
    }

    @Test
    fun `persists result correctly for positive result`() {
        every {
            persistenceService.persistPositiveTestResult(
                any(),
                any()
            )
        } returns VirologyResultPersistOperation.Success()

        val service = virologyService()

        val testResult = npexTestResultWith(Positive)

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistPositiveTestResult(testResult, fourWeeksExpireAt)
        }
    }

    @Test
    fun `persists result correctly for negative result`() {
        every { persistenceService.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService()

        val testResult = npexTestResultWith(Negative)

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistNonPositiveTestResult(testResult)
        }
    }

    @Test
    fun `persists result correctly for void result`() {
        every { persistenceService.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService()

        val testResult = npexTestResultWith(Void)

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistNonPositiveTestResult(testResult)
        }
    }

    @Test
    fun `accepts test lab virology positive result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("074qbxqq"),
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"),
        )
        every {
            persistenceService.persistTestOrderAndResult(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns testOrderTokens

        val service = virologyService()

        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Positive,
                LAB_RESULT
            )
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("074qbxqq")))
        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, Positive, TestEndDate.of(2020, 8, 7), any())
        }
    }

    @Test
    fun `accepts test lab virology negative result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("1e19z5zt"),
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0")
        )
        every {
            persistenceService.persistTestOrderAndResult(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns testOrderTokens

        val service = virologyService()
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 6, 7),
                Negative,
                LAB_RESULT
            )
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))
        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, Negative, TestEndDate.of(2020, 6, 7), any()
            )
        }
    }

    @Test
    fun `accepts test lab virology void result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("1e19z5zt"),
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0")
        )
        every {
            persistenceService.persistTestOrderAndResult(
                any(),
                any(),
                any(),
                any(),
                any(),

            )
        } returns testOrderTokens

        val service = virologyService()
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Void,
                LAB_RESULT
            )
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))
        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, Void, TestEndDate.of(2020, 8, 7), any()
            )
        }
    }

    @Test
    fun `exchanges cta token v1 with lfd test type is found`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokens = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveRapidResult

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistenceService.updateOnCtaExchange(any(), any(), any()) } just Runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = virologyService()
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequestV1(ctaToken))

        assertThat(result).isInstanceOf(CtaExchangeResult.Available::class.java)
        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
            persistenceService.updateOnCtaExchange(testTokens, testResult, any())
        }
    }

    private fun npexTestResultWith(testResult: TestResult) =
        VirologyResultRequestV2(
            CtaToken.of("cc8f0b6z"),
            TestEndDate.of(2020, 4, 23),
            testResult,
            LAB_RESULT
        )

    private fun virologyService(tokenGenerator: TokensGenerator = TokensGenerator) =
        VirologyService(persistenceService, tokenGenerator, clock, virologyPolicyConfig, events)
}
