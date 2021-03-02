package uk.nhs.nhsx.virology

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.exceptions.ApiResponseException
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV2
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyRequestType
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.VirologyLookupResult
import uk.nhs.nhsx.virology.result.VirologyResultRequest
import uk.nhs.nhsx.virology.result.VirologyResultRequest.FIORANO_INDETERMINATE
import uk.nhs.nhsx.virology.result.VirologyResultRequest.NonPositive
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Supplier

class VirologyServiceV2Test {

    private val now = Instant.ofEpochSecond(0)
    private val clock = Supplier { now }
    private val persistence = mockk<VirologyPersistenceService>()
    private val tokensGenerator = TokensGenerator()
    private val fourWeeksExpireAt = Duration.ofDays(4 * 7.toLong()).seconds
    private val ctaToken = CtaToken.of("cc8f0b6z")
    private val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
    private val country = Country.of("England")
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
            "cc8f0b6z", "polling-token", "submission-token"
        )

        val service = virologyService(tokenGenerator)
        val response = service.handleTestOrderRequest(websiteConfig, VirologyRequestType.ORDER)

        assertThat(response.diagnosisKeySubmissionToken).isEqualTo("submission-token")
        assertThat(response.testResultPollingToken).isEqualTo("polling-token")
        assertThat(response.tokenParameterValue).isEqualTo("cc8f0b6z")
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")

        verifySequence {
            persistence.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `lookup with result available`() {
        val testResult = TestData.positiveLabResult
        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs
        val service = virologyService(TokensGenerator())
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.virologyLookupForV2(request, mobileAppVersion)

        val resultAvailable = lookupResult as VirologyLookupResult.AvailableV2
        assertThat(resultAvailable.virologyLookupResponse.testEndDate).isEqualTo(testResult.testEndDate)
        assertThat(resultAvailable.virologyLookupResponse.testResult).isEqualTo(testResult.testResult)
        assertThat(resultAvailable.virologyLookupResponse.testKit).isEqualTo(LAB_RESULT)
        assertThat(resultAvailable.virologyLookupResponse.diagnosisKeySubmissionSupported).isTrue
        assertThat(resultAvailable.virologyLookupResponse.requiresConfirmatoryTest).isFalse

        verifySequence {
            persistence.getTestResult(pollingToken)
            persistence.markForDeletion(testResult, VirologyDataTimeToLive(
                Duration.ofHours(4).toMillis() / 1000,
                Duration.ofDays(4).toMillis() / 1000
            ))
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup negative lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.negativeResultFor(testKit)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.virologyLookupForV2(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.virologyLookupResponse.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.virologyLookupResponse.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup void lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.voidResultFor(testKit)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.virologyLookupForV2(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.virologyLookupResponse.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.virologyLookupResponse.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `lookup supporting diagnosis keys submission for each country`(country: String, expectedFlag: Boolean) {
        val testResult = TestData.positiveLabResult

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = VirologyLookupRequestV2(pollingToken, Country.of(country))
        val result = service.virologyLookupForV2(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.virologyLookupResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedFlag)
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `lookup requesting confirmatory test for each country`(country: String, expectedFlag: Boolean) {
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = VirologyLookupRequestV2(pollingToken, Country.of(country))
        val result = service.virologyLookupForV2(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.virologyLookupResponse.requiresConfirmatoryTest).isEqualTo(expectedFlag)
    }

    @Test
    fun `lookup with result pending`() {
        every { persistence.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)

        val service = virologyService()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.virologyLookupForV2(request, mobileAppVersion)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @Test
    fun `lookup pending when mobile version is considered old and confirmatory test required`() {
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.virologyLookupForV2(request, MobileAppVersion.Version(4, 3))

        assertThat(result).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @Test
    fun `lookup with no match`() {
        every { persistence.getTestResult(any()) } returns Optional.empty()

        val service = virologyService()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.virologyLookupForV2(request, mobileAppVersion)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.NotFound::class.java)
    }

    @Test
    fun `exchanges cta token`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token")
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionToken).isEqualTo("sub-token")
        assertThat(result.ctaExchangeResponse.testEndDate).isEqualTo("2020-04-23T18:34:03Z")
        assertThat(result.ctaExchangeResponse.testResult).isEqualTo("POSITIVE")
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

        val testOrder = TestOrder(ctaToken.value, pollingToken.value, "sub-token")

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.ctaExchangeResponse.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `exchanges cta token void lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.voidResultFor(testKit)

        val testOrder = TestOrder(ctaToken.value, pollingToken.value, "sub-token")

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = CtaExchangeRequestV2(ctaToken, country)
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.ctaExchangeResponse.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `exchanges cta token supporting diagnosis keys submission for each country`(country: String, expectedFlag: Boolean) {
        val testOrder = TestOrder(ctaToken.value, pollingToken.value, "sub-token")
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = CtaExchangeRequestV2(ctaToken, Country.of(country))
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedFlag)
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `exchanges cta token requesting confirmatory test for each country`(country: String, expectedFlag: Boolean) {
        val testOrder = TestOrder(ctaToken.value, pollingToken.value, "sub-token")
        val testResult = TestData.positiveResultFor(testOrder.testResultPollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val request = CtaExchangeRequestV2(ctaToken, Country.of(country))
        val result = service.exchangeCtaTokenForV2(request, mobileAppVersion) as CtaExchangeResult.AvailableV2

        assertThat(result.ctaExchangeResponse.requiresConfirmatoryTest).isEqualTo(expectedFlag)
    }

    @Test
    fun `exchanges cta token with pending test result`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokens = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token")
        val testResult = TestData.pendingTestResult

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.of(testResult)

        val service = virologyService(TokensGenerator())
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), mobileAppVersion)

        assertThat(result).isInstanceOf(CtaExchangeResult.Pending::class.java)
        verifySequence {
            persistence.getTestOrder(ctaToken)
            persistence.getTestResult(testResultPollingToken)
        }
    }

    @Test
    fun `exchanges cta token pending when mobile version is considered old and confirmatory test required`() {
        val testTokens = TestOrder(ctaToken.value, pollingToken.value, "sub-token")
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)

        val service = virologyService(TokensGenerator())
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), MobileAppVersion.Version(4, 3))

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
    }

    @Test
    fun `exchanges cta token and does not find match`() {
        every { persistence.getTestOrder(ctaToken) } returns Optional.empty()

        val service = virologyService(TokensGenerator())
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), mobileAppVersion)

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verifySequence { persistence.getTestOrder(ctaToken) }
    }

    @Test
    fun `exchanges cta token and does not find test result match`() {
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token")

        every { persistence.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.empty()

        val service = virologyService(TokensGenerator())
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
        val testTokensCounter = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token", 2)
        val testResult = TestData.positiveResultFor(testTokensCounter.testResultPollingToken)

        every { persistence.getTestOrder(ctaToken) }.returns(Optional.of(testTokensCounter))
        every { persistence.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyService(TokensGenerator())
        val result = service.exchangeCtaTokenForV2(CtaExchangeRequestV2(ctaToken, country), mobileAppVersion)

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verify(exactly = 1) { persistence.getTestOrder(ctaToken) }
    }

    @Test
    fun `persists result correctly for positive result`() {
        every { persistence.persistPositiveTestResult(any(), any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith("POSITIVE")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistPositiveTestResult(VirologyResultRequest.Positive.from(testResult), fourWeeksExpireAt)
        }
    }

    @Test
    fun `persists result correctly for negative result`() {
        every { persistence.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith("NEGATIVE")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistNonPositiveTestResult(NonPositive.from(testResult))
        }
    }

    @Test
    fun `persists result correctly for void result`() {
        every { persistence.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith("VOID")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistNonPositiveTestResult(NonPositive.from(testResult))
        }
    }

    @Test
    fun `persists result correctly for indeterminate result`() {
        every { persistence.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith("INDETERMINATE")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistence.persistNonPositiveTestResult(NonPositive.from(testResult))
        }
    }

    @Test
    fun `throws exception with invalid test result`() {
        val service = virologyService(tokensGenerator)

        val testResult = npexTestResultWith("ORANGE")

        assertThatThrownBy { service.acceptTestResult(testResult) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid test result value")
    }

    @Test
    fun `accepts test lab virology positive result`() {
        val testOrderTokens = TestOrder(
            "074qbxqq",
            "09657719-fe58-46a3-a3a3-a8db82d48043",
            "9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"
        )
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = virologyService(tokensGenerator)

        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest("POSITIVE", "2020-08-07T00:00:00Z")
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse.of("074qbxqq"))
        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, "POSITIVE", "2020-08-07T00:00:00Z", any()
            )
        }
    }

    @Test
    fun `accepts test lab virology negative result`() {
        val testOrderTokens = TestOrder(
            "1e19z5zt",
            "09657719-fe58-46a3-a3a3-a8db82d48043",
            "9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"
        )
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = virologyService(tokensGenerator)
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest("NEGATIVE", "2020-06-07T00:00:00Z")
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse.of("1e19z5zt"))
        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, "NEGATIVE", "2020-06-07T00:00:00Z", any()
            )
        }
    }

    @Test
    fun `accepts test lab virology void result`() {
        val testOrderTokens = TestOrder(
            "1e19z5zt",
            "09657719-fe58-46a3-a3a3-a8db82d48043",
            "9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"
        )
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = virologyService(tokensGenerator)
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest("VOID", "2020-06-07T00:00:00Z")
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse.of("1e19z5zt"))
        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, "VOID", "2020-06-07T00:00:00Z", any()
            )
        }
    }

    @Test
    fun `accepts test lab virology indeterminate result converting to void`() {
        val testOrderTokens = TestOrder(
            "1e19z5zt",
            "09657719-fe58-46a3-a3a3-a8db82d48043",
            "9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"
        )
        every { persistence.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = virologyService(tokensGenerator)
        service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest(FIORANO_INDETERMINATE, "2020-06-07T00:00:00Z")
        )

        verify(exactly = 1) {
            persistence.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, "VOID", "2020-06-07T00:00:00Z", any()
            )
        }
    }

    @Test
    fun `throws test lab virology when result is not valid`() {
        val service = virologyService(tokensGenerator)

        assertThatThrownBy {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("unexpected-value", "2020-06-07T00:00:00Z")
            )
        }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid test result value")
    }

    private fun virologyService(tokenGenerator: TokensGenerator = TokensGenerator()) =
        VirologyService(persistence, tokenGenerator, clock, virologyPolicyConfig, RecordingEvents())

    private fun npexTestResultWith(testResult: String) =
        VirologyResultRequest("cc8f0b6z", "2020-04-23T00:00:00Z", testResult)
}
