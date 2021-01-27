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
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.exceptions.ApiResponseException
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequest
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequest
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

class VirologyServiceTest {

    private val now = Instant.ofEpochSecond(0)
    private val clock = Supplier { now }
    private val persistenceService = mockk<VirologyPersistenceService>()
    private val tokensGenerator = TokensGenerator()
    private val fourWeeksExpireAt = Duration.ofDays(4 * 7.toLong()).seconds
    private val websiteConfig = VirologyWebsiteConfig(
        "https://example.order-a-test.uk", "https://example.register-a-test.uk"
    )

    @Test
    fun `order virology test`() {
        val tokenGenerator = mockk<TokensGenerator>()
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            "cc8f0b6z", "polling-token", "submission-token"
        )

        val service = VirologyService(
            persistenceService,
            tokenGenerator,
            clock
        )

        val response = service.handleTestOrderRequest(websiteConfig, VirologyRequestType.ORDER)

        assertThat(response.diagnosisKeySubmissionToken).isEqualTo("submission-token")
        assertThat(response.testResultPollingToken).isEqualTo("polling-token")
        assertThat(response.tokenParameterValue).isEqualTo("cc8f0b6z")
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")

        verifySequence {
            persistenceService.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `register for virology test`() {
        val tokenGenerator = mockk<TokensGenerator>()
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            "cc8f0b6z", "polling-token", "submission-token"
        )

        val service = VirologyService(
            persistenceService,
            tokenGenerator,
            clock
        )

        val response = service.handleTestOrderRequest(websiteConfig, VirologyRequestType.REGISTER)

        assertThat(response.diagnosisKeySubmissionToken).isEqualTo("submission-token")
        assertThat(response.testResultPollingToken).isEqualTo("polling-token")
        assertThat(response.tokenParameterValue).isEqualTo("cc8f0b6z")
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://example.register-a-test.uk?ctaToken=cc8f0b6z")

        verify(exactly = 1) {
            persistenceService.persistTestOrder(any(), fourWeeksExpireAt)
        }
    }

    @Test
    fun `virology lookup with result available`() {
        val testResult = TestData.positivePcrTestResult
        every { persistenceService.getTestResult(any()) } returns Optional.of(testResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs
        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            clock
        )
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequest(pollingToken)
        val lookupResult = service.virologyLookupForV1(request)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Available::class.java)
        val resultAvailable = lookupResult as VirologyLookupResult.Available
        assertThat(resultAvailable.virologyLookupResponse.testEndDate).isEqualTo(testResult.testEndDate)
        assertThat(resultAvailable.virologyLookupResponse.testResult).isEqualTo(testResult.testResult)

        verifySequence {
            persistenceService.getTestResult(pollingToken)
            persistenceService.markForDeletion(testResult, VirologyDataTimeToLive(
                Duration.ofHours(4).toMillis() / 1000,
                Duration.ofDays(4).toMillis() / 1000
            ))
        }
    }

    @Test
    fun `virology lookup with result pending`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResultNoTestKit)

        val service = VirologyService(persistenceService, TokensGenerator(), SystemClock.CLOCK)
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequest(pollingToken)

        val lookupResult = service.virologyLookupForV1(request)
        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @Test
    fun `virology lookup with no match`() {
        every { persistenceService.getTestResult(any()) } returns Optional.empty()

        val service = VirologyService(persistenceService, TokensGenerator(), SystemClock.CLOCK)
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequest(pollingToken)

        val lookupResult = service.virologyLookupForV1(request)
        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.NotFound::class.java)
    }

    @Test
    fun `exchanges cta token`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token")
        val testResult = TestData.positiveTestResultFor(testOrder.testResultPollingToken)

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistenceService.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = VirologyService(persistenceService, TokensGenerator(), clock)
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequest(ctaToken)) as CtaExchangeResult.Available

        assertThat(result.ctaExchangeResponse.diagnosisKeySubmissionToken).isEqualTo("sub-token")
        assertThat(result.ctaExchangeResponse.testEndDate).isEqualTo("2020-04-23T18:34:03Z")
        assertThat(result.ctaExchangeResponse.testResult).isEqualTo("POSITIVE")

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
        val testTokens = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token")
        val testResult = TestData.pendingTestResultNoTestKit

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)

        val service = VirologyService(persistenceService, TokensGenerator(), clock)
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequest(ctaToken))

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

        val service = VirologyService(persistenceService, TokensGenerator(), clock)
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequest(ctaToken))

        assertThat(result).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        verifySequence { persistenceService.getTestOrder(ctaToken) }
    }

    @Test
    fun `exchanges cta token and does not find test result match`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testOrder = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token")

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testOrder)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.empty()

        val service = VirologyService(persistenceService, TokensGenerator(), clock)
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequest(ctaToken))

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
        val testTokensCounter = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token", 2)
        val testResult = TestData.positiveTestResultFor(testTokensCounter.testResultPollingToken)

        every { persistenceService.getTestOrder(ctaToken) }.returns(
            (Optional.of(testTokensCounter))
        )
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = VirologyService(persistenceService, TokensGenerator(), clock)

        assertThat(service.exchangeCtaTokenForV1(CtaExchangeRequest(ctaToken))).isInstanceOf(CtaExchangeResult.NotFound::class.java)

        verify(exactly = 1) { persistenceService.getTestOrder(ctaToken) }
    }

    @Test
    fun `persists result correctly for positive result`() {
        every { persistenceService.persistPositiveTestResult(any(), any()) } returns VirologyResultPersistOperation.Success()

        val service = VirologyService(persistenceService, tokensGenerator, clock)

        val testResult = npexTestResultWith("POSITIVE")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistPositiveTestResult(VirologyResultRequest.Positive.from(testResult), fourWeeksExpireAt)
        }
    }

    @Test
    fun `persists result correctly for negative result`() {
        every { persistenceService.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = VirologyService(persistenceService, tokensGenerator, clock)

        val testResult = npexTestResultWith("NEGATIVE")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistNonPositiveTestResult(NonPositive.from(testResult))
        }
    }

    @Test
    fun `persists result correctly for void result`() {
        every { persistenceService.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = VirologyService(persistenceService, tokensGenerator, clock)

        val testResult = npexTestResultWith("VOID")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistNonPositiveTestResult(NonPositive.from(testResult))
        }
    }

    @Test
    fun `persists result correctly for indeterminate result`() {
        every { persistenceService.persistNonPositiveTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val service = VirologyService(persistenceService, tokensGenerator, clock)

        val testResult = npexTestResultWith("INDETERMINATE")

        service.acceptTestResult(testResult)

        verify(exactly = 1) {
            persistenceService.persistNonPositiveTestResult(NonPositive.from(testResult))
        }
    }

    @Test
    fun `throws exception with invalid test result`() {
        val service = VirologyService(persistenceService, tokensGenerator, clock)

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
        every { persistenceService.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = VirologyService(persistenceService, tokensGenerator, clock)

        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest("POSITIVE", "2020-08-07T00:00:00Z")
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse.of("074qbxqq"))
        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
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
        every { persistenceService.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = VirologyService(persistenceService, tokensGenerator, clock)
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest("NEGATIVE", "2020-06-07T00:00:00Z")
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse.of("1e19z5zt"))
        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
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
        every { persistenceService.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = VirologyService(persistenceService, tokensGenerator, clock)
        val response = service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest("VOID", "2020-06-07T00:00:00Z")
        )

        assertThat(response).isEqualTo(VirologyTokenGenResponse.of("1e19z5zt"))
        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
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
        every { persistenceService.persistTestOrderAndResult(any(), any(), any(), any(), any()) } returns testOrderTokens

        val service = VirologyService(persistenceService, tokensGenerator, clock)
        service.acceptTestResultGeneratingTokens(
            VirologyTokenGenRequest(FIORANO_INDETERMINATE, "2020-06-07T00:00:00Z")
        )

        verify(exactly = 1) {
            persistenceService.persistTestOrderAndResult(
                any(), fourWeeksExpireAt, "VOID", "2020-06-07T00:00:00Z", any()
            )
        }
    }

    @Test
    fun `throws test lab virology when result is not valid`() {
        val service = VirologyService(persistenceService, tokensGenerator, clock)

        assertThatThrownBy {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("unexpected-value", "2020-06-07T00:00:00Z")
            )
        }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid test result value")
    }

    @Test
    fun `virology lookup for v1 with lfd test type is found`() {
        val testResult = TestData.positiveLfdTestResult
        every { persistenceService.getTestResult(any()) } returns Optional.of(testResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs
        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            clock
        )
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequest(pollingToken)
        val lookupResult = service.virologyLookupForV1(request)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Available::class.java)

        verify {
            persistenceService.getTestResult(pollingToken)
        }
    }

    @Test
    fun `exchanges cta token v1 with lfd test type is found`() {
        val ctaToken = CtaToken.of("cc8f0b6z")
        val testResultPollingToken = TestResultPollingToken.of("poll-token")
        val testTokens = TestOrder(ctaToken.value, testResultPollingToken.value, "sub-token")
        val testResult = TestData.positiveLfdTestResult

        every { persistenceService.getTestOrder(ctaToken) } returns Optional.of(testTokens)
        every { persistenceService.getTestResult(testResultPollingToken) } returns Optional.of(testResult)
        every { persistenceService.updateOnCtaExchange(any(), any(), any()) } just Runs

        val service = VirologyService(persistenceService, TokensGenerator(), clock)
        val result = service.exchangeCtaTokenForV1(CtaExchangeRequest(ctaToken))

        assertThat(result).isInstanceOf(CtaExchangeResult.Available::class.java)
        verifySequence {
            persistenceService.getTestOrder(ctaToken)
            persistenceService.getTestResult(testResultPollingToken)
            persistenceService.updateOnCtaExchange(testTokens, testResult, any())
        }
    }

    private fun npexTestResultWith(testResult: String) =
        VirologyResultRequest("cc8f0b6z", "2020-04-23T00:00:00Z", testResult)

}
