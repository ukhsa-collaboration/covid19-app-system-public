package uk.nhs.nhsx.virology

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.virology.Country.Companion.England
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyRequestType
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import uk.nhs.nhsx.virology.result.TestResult.Negative
import uk.nhs.nhsx.virology.result.TestResult.Positive
import uk.nhs.nhsx.virology.result.TestResult.Void
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Instant
import java.time.Period
import java.util.*
import java.util.function.Supplier

class VirologyServiceV2Test {

    private val now = Instant.EPOCH
    private val clock = Supplier { now }
    private val persistence = mockk<VirologyPersistenceService>()
    private val tokensGenerator = TokensGenerator
    private val fourWeeksExpireAt = now.plus(Period.ofWeeks(4))
    private val ctaToken = CtaToken.of("cc8f0b6z")
    private val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
    private val country = England
    private val virologyPolicyConfig = VirologyPolicyConfig()
    private val mobileAppVersion = MobileAppVersion.Version(5, 0)
    private val websiteConfig = VirologyWebsiteConfig(
        "https://example.order-a-test.uk",
        "https://example.register-a-test.uk"
    )

    @Test
    fun `order virology test`() {
        val tokenGenerator = mockk<TokensGenerator>()
        every { persistence.persistTestOrder(any(), any()) } returns TestOrder(
            CtaToken.of("cc8f0b6z"), TestResultPollingToken.of("poll-token"), DiagnosisKeySubmissionToken.of("submission-token")
        )

        val service = virologyService(tokenGenerator)
        val response = service.handleTestOrderRequest(websiteConfig, VirologyRequestType.ORDER)

        assertThat(response.diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("submission-token"))
        assertThat(response.testResultPollingToken).isEqualTo(TestResultPollingToken.of("poll-token"))
        assertThat(response.tokenParameterValue).isEqualTo(CtaToken.of("cc8f0b6z"))
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")

        verifySequence {
            persistence.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `exchanges cta token`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator)
        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("sub-token"))
        assertThat(result.ctaExchangeResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(result.ctaExchangeResponse.testResult).isEqualTo(Positive)
        assertThat(result.ctaExchangeResponse.testKit).isEqualTo(LAB_RESULT)
        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionSupported).isTrue
        assertThat(result.ctaExchangeResponse.requiresConfirmatoryTest).isFalse

        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
            persistence.updateOnCtaExchange(testOrder, testResult, any())
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `exchanges cta token negative lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.negativeResultFor(testKit)

        val testOrder = TestOrder(ctaToken, pollingToken, DiagnosisKeySubmissionToken.of("sub-token"))

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator)
        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.ctaExchangeResponse.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `exchanges cta token void lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.voidResultFor(testKit)

        val testOrder = TestOrder(ctaToken, pollingToken, DiagnosisKeySubmissionToken.of("sub-token"))

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator)
        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.ctaExchangeResponse.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `exchanges cta token supporting diagnosis keys submission for each country`(
        country: String,
        expectedFlag: Boolean
    ) {
        val testOrder = TestOrder(ctaToken, pollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator)
        val request = CtaExchangeRequestV2(ctaToken, Country.of(country))
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedFlag)
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `exchanges cta token requesting confirmatory test for each country`(country: String, expectedFlag: Boolean) {
        val testOrder = TestOrder(ctaToken, pollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator)
        val request = CtaExchangeRequestV2(ctaToken, Country.of(country))
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.requiresConfirmatoryTest).isEqualTo(expectedFlag)
    }

    @Test
    fun `exchanges cta token with pending test result`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokens = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.pendingTestResult

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.of(testResult)

        val service = virologyService(TokensGenerator)
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), mobileAppVersion)

        assertThat(result).isInstanceOf(CtaExchangeResult.Pending::class.java)
        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
        }
    }

    @Test
    fun `exchanges cta token pending when mobile version is considered old and confirmatory test required`() {
        val testTokens = TestOrder(ctaToken, pollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)

        val service = virologyService(TokensGenerator)
        val result =
            service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), MobileAppVersion.Version(4, 3))

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
    }

    @Test
    fun `exchanges cta token and does not find match`() {
        every { persistence.getTestOrder(ctaToken) } returns Optional.empty()

        val service = virologyService(TokensGenerator)
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), mobileAppVersion)

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verifySequence { persistence.getTestOrder(ctaToken) }
    }

    @Test
    fun `exchanges cta token and does not find test result match`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.empty()

        val service = virologyService(TokensGenerator)
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), mobileAppVersion)

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
        }
    }

    @Test
    fun `when download counter is 2, return not found`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokensCounter = TestOrder(ctaToken, 2, testResultPollingToken, DiagnosisKeySubmissionToken.of("sub-token"))
        val testResult = TestData.positiveResultFor(testTokensCounter.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) }.returns(Optional.of(testTokensCounter))
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyService(TokensGenerator)
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), mobileAppVersion)

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verify(exactly = 1) { persistence.getTestOrder(ctaToken) }
    }

    @Test
    fun `persists result correctly for positive result`() {
        every { persistence.persistPositiveTestResult(any(), any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith(Positive)

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistPositiveTestResult(testResult, fourWeeksExpireAt)
        }
    }

    @Test
    fun `persists result correctly for negative result`() {
        every { persistence.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith(Negative)

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistNonPositiveTestResult(testResult)
        }
    }

    @Test
    fun `persists result correctly for void result`() {
        every { persistence.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith(Void)

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistNonPositiveTestResult(testResult)
        }
    }

    @Test
    fun `accepts test lab virology positive result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("074qbxqq"),
            TestResultPollingToken.of( "09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0")
        )
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = virologyService(tokensGenerator)

        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Positive,
                LAB_RESULT
            )
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("074qbxqq")))
        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, Positive, TestEndDate.of(2020, 8, 7), any()
            )
        }
    }

    @Test
    fun `accepts test lab virology negative result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("1e19z5zt"),
            TestResultPollingToken.of( "09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0")
        )
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = virologyService(tokensGenerator)
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Negative,
                LAB_RESULT
            )
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))
        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, Negative, TestEndDate.of(2020, 8, 7), any()
            )
        }
    }

    @Test
    fun `accepts test lab virology void result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("1e19z5zt"),
            TestResultPollingToken.of( "09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0")
        )
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = virologyService(tokensGenerator)
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Void,
                LAB_RESULT
            )
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))
        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, Void, TestEndDate.of(2020, 8, 7), any()
            )
        }
    }

    private fun virologyService(tokenGenerator: TokensGenerator = TokensGenerator) =
        VirologyService(persistence, tokenGenerator, clock, virologyPolicyConfig, RecordingEvents())

    private fun npexTestResultWith(testResult: TestResult) =
        VirologyResultRequestV2(
            CtaToken.of("cc8f0b6z"),
            TestEndDate.of(2020, 4, 23),
            testResult,
            LAB_RESULT
        )
}
