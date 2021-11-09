@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.virology

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Plod
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.DownloadCountExceeded
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.PolicyRejectionV2
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.TestOrderNotFound
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.TestResultNotFound
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource.Eng
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.AvailableV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Pending
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.order.VirologyRequestType.ORDER
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.Success
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import uk.nhs.nhsx.virology.result.VirologyTokenStatusRequest
import uk.nhs.nhsx.virology.result.VirologyTokenStatusResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period

class VirologyServiceV2Test {

    private val events = RecordingEvents()
    private val now = Instant.EPOCH
    private val clock = { now }
    private val persistence = mockk<VirologyPersistenceService>()
    private val tokensGenerator = TokensGenerator
    private val fourWeeksExpireAt = now.plus(Period.ofWeeks(4))
    private val ctaToken = CtaToken.of("cc8f0b6z")
    private val ctaTokenAsString = ctaToken.value
    private val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
    private val country = England
    private val virologyPolicyConfig = VirologyPolicyConfig()
    private val mobileAppVersion = MobileAppVersion.Version(5, 0)
    private val mobileOS = MobileOS.iOS
    private val websiteConfig = VirologyWebsiteConfig(
        "https://example.order-a-test.uk",
        "https://example.register-a-test.uk"
    )

    @Test
    fun `order virology test`() {
        val tokenGenerator = mockk<TokensGenerator>()

        every { persistence.persistTestOrder(any(), any()) } returns TestOrder(
            ctaToken,
            TestResultPollingToken.of("poll-token"),
            DiagnosisKeySubmissionToken.of("submission-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val response = VirologyService(tokenGenerator).handleTestOrderRequest(websiteConfig, ORDER)

        expectThat(response) {
            get(VirologyOrderResponse::diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("submission-token"))
            get(VirologyOrderResponse::testResultPollingToken).isEqualTo(TestResultPollingToken.of("poll-token"))
            get(VirologyOrderResponse::tokenParameterValue).isEqualTo(ctaToken)
            get(VirologyOrderResponse::websiteUrlWithQuery).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")
        }

        verifySequence {
            persistence.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `exchanges cta token`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")

        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(testResultPollingToken) } returns testResult
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just runs

        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            request = CtaExchangeRequestV2(ctaToken, country),
            mobileAppVersion = mobileAppVersion,
            mobileOS = mobileOS
        )

        expectThat(result)
            .isA<AvailableV2>()
            .get(AvailableV2::ctaExchangeResponse).and {
                get(CtaExchangeResponseV2::diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of("sub-token"))
                get(CtaExchangeResponseV2::testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
                get(CtaExchangeResponseV2::testResult).isEqualTo(Positive)
                get(CtaExchangeResponseV2::testKit).isEqualTo(LAB_RESULT)
                get(CtaExchangeResponseV2::diagnosisKeySubmissionSupported).isTrue()
                get(CtaExchangeResponseV2::requiresConfirmatoryTest).isFalse()
            }

        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
            persistence.updateOnCtaExchange(testOrder, testResult, any())
        }

        expectThat(events).contains(SuccessfulCtaExchange::class, CtaExchangeCompleted::class)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `exchanges cta token negative lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.negativeResultFor(testKit)

        val testOrder = TestOrder(
            ctaToken,
            pollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(pollingToken) } returns testResult
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just runs

        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            request = request,
            mobileAppVersion = mobileAppVersion,
            mobileOS = mobileOS
        )

        expectThat(result)
            .isA<AvailableV2>()
            .get(AvailableV2::ctaExchangeResponse).and {
                get(CtaExchangeResponseV2::diagnosisKeySubmissionSupported).isFalse()
                get(CtaExchangeResponseV2::requiresConfirmatoryTest).isFalse()
            }

        expectThat(events).contains(CtaExchangeCompleted::class)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `exchanges cta token void lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.voidResultFor(testKit)

        val testOrder = TestOrder(
            ctaToken,
            pollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(pollingToken) } returns testResult
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just runs

        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            request = request,
            mobileAppVersion = mobileAppVersion,
            mobileOS = mobileOS
        )

        expectThat(result)
            .isA<AvailableV2>()
            .get(AvailableV2::ctaExchangeResponse).and {
                get(CtaExchangeResponseV2::diagnosisKeySubmissionSupported).isFalse()
                get(CtaExchangeResponseV2::requiresConfirmatoryTest).isFalse()
            }

        expectThat(events).contains(CtaExchangeCompleted::class)
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `exchanges cta token supporting diagnosis keys submission for each country`(
        country: String,
        expectedFlag: Boolean
    ) {
        val testOrder = TestOrder(
            ctaToken,
            pollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(pollingToken) } returns testResult
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just runs

        val request = CtaExchangeRequestV2(ctaToken, Country.of(country))
        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            request = request,
            mobileAppVersion = mobileAppVersion,
            mobileOS = mobileOS
        )

        expectThat(result)
            .isA<AvailableV2>()
            .get(AvailableV2::ctaExchangeResponse).and {
                get(CtaExchangeResponseV2::diagnosisKeySubmissionSupported).isEqualTo(expectedFlag)
            }

        expectThat(events).contains(SuccessfulCtaExchange::class, CtaExchangeCompleted::class)
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `exchanges cta token requesting confirmatory test for each country`(country: String, expectedFlag: Boolean) {
        val testOrder = TestOrder(
            ctaToken,
            pollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(pollingToken) } returns testResult
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just runs

        val request = CtaExchangeRequestV2(ctaToken, Country.of(country))
        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            request = request,
            mobileAppVersion = mobileAppVersion,
            mobileOS = mobileOS
        ) as AvailableV2

        expectThat(result)
            .isA<AvailableV2>()
            .get(AvailableV2::ctaExchangeResponse).and {
                get(CtaExchangeResponseV2::requiresConfirmatoryTest).isEqualTo(expectedFlag)
            }

        expectThat(events).contains(SuccessfulCtaExchange::class, CtaExchangeCompleted::class)
    }

    @Test
    fun `exchanges cta token with pending test result`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokens = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.pendingTestResult

        every { persistence.getTestOrder(ctaToken) } returns testTokens
        every { persistence.getTestResult(testResultPollingToken) } returns testResult

        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            CtaExchangeRequestV2(ctaToken, country),
            mobileAppVersion,
            mobileOS
        )

        expectThat(result).isA<Pending>()

        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
        }

        expectThat(events).contains(VirologyResultPending::class)
    }

    @Test
    fun `exchanges cta token pending when mobile version is considered old and confirmatory test required`() {
        val testOrder = TestOrder(
            ctaToken,
            pollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(pollingToken) } returns testResult

        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            CtaExchangeRequestV2(ctaToken, country),
            MobileAppVersion.Version(4, 3),
            mobileOS
        )

        expectThat(result).isA<NotFound>()

        expectThat(events).contains(PolicyRejectionV2::class)
    }

    @Test
    fun `exchanges cta token and does not find match`() {
        every { persistence.getTestOrder(ctaToken) } returns null

        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            CtaExchangeRequestV2(ctaToken, country),
            mobileAppVersion,
            mobileOS
        )

        expectThat(result).isA<NotFound>()

        verifySequence { persistence.getTestOrder(ctaToken) }

        expectThat(events).contains(TestOrderNotFound::class)
    }

    @Test
    fun `exchanges cta token and does not find test result match`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(testResultPollingToken) } returns null

        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            CtaExchangeRequestV2(ctaToken, country),
            mobileAppVersion,
            mobileOS
        )

        expectThat(result).isA<NotFound>()

        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
        }

        expectThat(events).contains(TestResultNotFound::class)
    }

    @Test
    fun `when download counter is 2, return not found`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            2,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(testResultPollingToken) } returns testResult
        every { persistence.markForDeletion(any(), any()) } just runs

        val result = VirologyService(TokensGenerator).exchangeCtaTokenForV2(
            CtaExchangeRequestV2(ctaToken, country),
            mobileAppVersion,
            mobileOS
        )

        expectThat(result).isA<NotFound>()

        verify(exactly = 1) { persistence.getTestOrder(ctaToken) }

        expectThat(events).contains(DownloadCountExceeded::class)
    }

    @Test
    fun `persists positive result`() {
        every {
            persistence.persistTestResultWithKeySubmission(any(), any())
        } returns Success()

        val testResult = NPEXTestResultWith(Positive)

        VirologyService(tokensGenerator).acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistTestResultWithKeySubmission(testResult, fourWeeksExpireAt)
        }
    }

    @Test
    fun `persists negative result`() {
        every { persistence.persistTestResultWithoutKeySubmission(any()) } returns Success()

        val testResult = NPEXTestResultWith(Negative)

        VirologyService(tokensGenerator).acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistTestResultWithoutKeySubmission(testResult)
        }
    }

    @Test
    fun `persists void result`() {
        every { persistence.persistTestResultWithoutKeySubmission(any()) } returns Success()

        val testResult = NPEXTestResultWith(Void)

        VirologyService(tokensGenerator).acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistTestResultWithoutKeySubmission(testResult)
        }
    }

    @Test
    fun `persists plod result`() {
        every { persistence.persistTestResultWithoutKeySubmission(any()) } returns Success()

        val testResult = NPEXTestResultWith(Plod)

        VirologyService(tokensGenerator).acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistTestResultWithoutKeySubmission(testResult)
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
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val response = VirologyService(tokensGenerator).acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Positive,
                LAB_RESULT
            )
        )

        expectThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("074qbxqq")))

        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
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

        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val response = VirologyService(tokensGenerator).acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Negative,
                LAB_RESULT
            )
        )

        expectThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))

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
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val response = VirologyService(tokensGenerator).acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Void,
                LAB_RESULT
            )
        )

        expectThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))

        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = fourWeeksExpireAt,
                testResult = Void,
                testEndDate = TestEndDate.of(2020, 8, 7),
                testKit = any()
            )
        }
    }

    @Test
    fun `accepts test lab virology plod result`() {
        val testOrderTokens = TestOrder(
            CtaToken.of("1e19z5zt"),
            TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
            DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val response = VirologyService(tokensGenerator).acceptTestResultGeneratingTokens(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 8, 7),
                Plod,
                LAB_RESULT
            )
        )

        expectThat(response).isEqualTo(VirologyTokenGenResponse(CtaToken.of("1e19z5zt")))

        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                testOrderFn = any(),
                expireAt = fourWeeksExpireAt,
                testResult = Plod,
                testEndDate = TestEndDate.of(2020, 8, 7),
                testKit = any()
            )
        }
    }

    @Test
    fun `check token returns consumable when ctaToken is in the database and has not been consumed`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistence.getTestOrder(ctaToken) } returns testOrder
        every { persistence.getTestResult(testResultPollingToken) } returns AvailableTestResult(
            testResultPollingToken,
            TestEndDate.of(2020, 5, 4),
            Positive,
            RAPID_SELF_REPORTED
        )

        val request = VirologyTokenStatusRequest(ctaTokenAsString)
        val result = VirologyService(TokensGenerator).checkStatusOfToken(request, Eng)

        expectThat(result).get(VirologyTokenStatusResponse::tokenStatus).isEqualTo("consumable")

        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
        }
    }

    @Test
    fun `check token returns other when ctaToken is in the database and has been consumed`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(
            ctaToken,
            1,
            testResultPollingToken,
            DiagnosisKeySubmissionToken.of("sub-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        every { persistence.getTestOrder(ctaToken) } returns testOrder

        val request = VirologyTokenStatusRequest(ctaTokenAsString)
        val result = VirologyService(TokensGenerator).checkStatusOfToken(request, Eng)

        expectThat(result).get(VirologyTokenStatusResponse::tokenStatus).isEqualTo("other")

        verifySequence {
            persistence.getTestOrder(ctaToken)
        }
    }

    @Test
    fun `check token returns other when ctaToken is not in the database`() {
        every { persistence.getTestOrder(ctaToken) } returns null

        val request = VirologyTokenStatusRequest(ctaTokenAsString)
        val result = VirologyService(TokensGenerator).checkStatusOfToken(request, Eng)

        expectThat(result).get(VirologyTokenStatusResponse::tokenStatus).isEqualTo("other")

        verifySequence {
            persistence.getTestOrder(ctaToken)
        }
    }

    @Test
    fun `check token returns other when ctaToken is not valid`() {
        val service = VirologyService(TokensGenerator)
        val request = VirologyTokenStatusRequest("invalidCtaToken")
        val result = service.checkStatusOfToken(request, Eng)

        expectThat(result).get(VirologyTokenStatusResponse::tokenStatus).isEqualTo("other")

        verify(exactly = 0) {
            persistence.getTestOrder(ctaToken)
        }
    }

    private fun VirologyService(tokenGenerator: TokensGenerator = TokensGenerator) = VirologyService(
        persistence = persistence,
        tokensGenerator = tokenGenerator,
        clock = clock,
        policyConfig = virologyPolicyConfig,
        events = events
    )

    private fun NPEXTestResultWith(testResult: TestResult) = VirologyResultRequestV2(
        ctaToken = CtaToken.of("cc8f0b6z"),
        testEndDate = TestEndDate.of(2020, 4, 23),
        testResult = testResult,
        testKit = LAB_RESULT
    )
}
