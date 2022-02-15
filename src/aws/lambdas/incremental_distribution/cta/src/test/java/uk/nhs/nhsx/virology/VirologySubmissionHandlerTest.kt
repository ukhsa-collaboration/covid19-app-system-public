@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.virology

import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.events.VirologyCtaExchange
import uk.nhs.nhsx.core.events.VirologyOrder
import uk.nhs.nhsx.core.events.VirologyRegister
import uk.nhs.nhsx.core.events.VirologyResults
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileAppVersion.Unknown
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.headers
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withHeader
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.UnprocessableVirologyCtaExchange
import uk.nhs.nhsx.virology.VirologySubmissionHandlerTest.ApiVersion.V1
import uk.nhs.nhsx.virology.VirologySubmissionHandlerTest.ApiVersion.V2
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupService
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import java.time.Duration
import java.time.LocalDateTime

class VirologySubmissionHandlerTest {

    private val contentSigner = mockk<Signer> {
        every { sign(any()) } returns
            Signature(
                KeyId.of("TEST_KEY_ID"),
                SigningAlgorithmSpec.ECDSA_SHA_256, "TEST_SIGNATURE".toByteArray()
            )
    }

    private val events = RecordingEvents()
    private val signer = AwsResponseSigner(RFC2616DatedSigner(CLOCK, contentSigner), events)
    private val websiteConfig = VirologyWebsiteConfig(
        "https://example.order-a-test.uk",
        "https://example.register-a-test.uk"
    )
    private val throttleDuration = Duration.ofMillis(1)
    private val authenticator = Authenticator { true }
    private val environment = TEST.apply(
        mapOf(
            "MAINTENANCE_MODE" to "false",
            "custom_oai" to "OAI",
        )
    )
    private val persistence = mockk<VirologyPersistenceService>()
    private val tokenGenerator = mockk<TokensGenerator>()
    private val virologyPolicyConfig = mockk<VirologyPolicyConfig>()
    private val country = England

    @Test
    fun `handle test result request success`() {
        every { persistence.getTestResult(any()) } returns TestData.positiveLabResult
        every { persistence.markForDeletion(any(), any()) } just runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))

        val response = VirologySubmissionHandler(virology)
            .handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson("""{"testEndDate":"2020-04-23T00:00:00Z","testResult":"POSITIVE","testKit":"LAB_RESULT"}""")
        }

        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `handle pending test result request success no content`() {
        every { persistence.getTestResult(any()) } returns TestData.pendingTestResult
        every { persistence.markForDeletion(any(), any()) } just runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NO_CONTENT)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `handle test result request missing token`() {
        every { persistence.getTestResult(any()) } returns TestData.pendingTestResult
        every { persistence.markForDeletion(any(), any()) } just runs

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("""{"testResultPollingToken":""}""")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `handle test result request null token`() {
        every { persistence.getTestResult(any()) } returns TestData.pendingTestResult
        every { persistence.markForDeletion(any(), any()) } just runs

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("""{"testResultPollingToken":null}""")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `handle test result request that does not exist`() {
        every { persistence.getTestResult(any()) } returns null

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NOT_FOUND)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualTo("Test result lookup submitted for unknown testResultPollingToken")
        }

        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `handle test result request for incorrect request json`() {
        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("""{"invalidField":"98cff3dd-882c-417b-a00a-350a205378c7"}""")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `handle test result request for missing body`() {
        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(VirologyResults::class, UnprocessableJson::class)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/virology-test/home-kit/order", "/virology-test/v2/order"])
    fun `handle test order request success`(orderUrl: String) {
        every { persistence.persistTestOrder(any(), any()) } returns TestOrder(
            CtaToken.of("cc8f0b6z"),
            TestResultPollingToken.of("polling-token"),
            DiagnosisKeySubmissionToken.of("submission-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath(orderUrl)
            .withBearerToken("anything")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson(
                """
                {
                    "websiteUrlWithQuery": "https://example.order-a-test.uk?ctaToken=cc8f0b6z",
                    "tokenParameterValue": "cc8f0b6z",
                    "testResultPollingToken": "polling-token",
                    "diagnosisKeySubmissionToken": "submission-token",
                    "venueHistorySubmissionToken": ""
                }
            """
            )
        }

        expectThat(events).contains(VirologyOrder::class)
    }

    @Test
    fun `handle test register request success`() {
        every { persistence.persistTestOrder(any(), any()) } returns TestOrder(
            CtaToken.of("cc8f0b6z"),
            TestResultPollingToken.of("polling-token"),
            DiagnosisKeySubmissionToken.of("submission-token"),
            LocalDateTime.now().plusWeeks(4)
        )

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/home-kit/register")
            .withBearerToken("anything")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson(
                """
                {
                    "websiteUrlWithQuery": "https://example.register-a-test.uk?ctaToken=cc8f0b6z",
                    "tokenParameterValue": "cc8f0b6z",
                    "testResultPollingToken": "polling-token",
                    "diagnosisKeySubmissionToken": "submission-token",
                    "venueHistorySubmissionToken": ""
                }
                """
            )
        }

        expectThat(events).contains(VirologyRegister::class)
    }

    @Test
    fun `handle unknown path`() {
        val virology = VirologyService(mockk())

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/unknown/path")
            .withBearerToken("anything")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NOT_FOUND)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }
    }

    @Test
    fun `exchange invalid cta token`() {
        val virology = mockk<VirologyService>()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(""" "{"ctaToken":null} """)

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(BAD_REQUEST)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(UnprocessableVirologyCtaExchange::class)
    }

    @Test
    fun `exchange cta token for available test result`() {
        val virology = mockk<VirologyService> {
            every { exchangeCtaTokenForV1(any(), any(), any()) } returns
                CtaExchangeResult.AvailableV1(
                    CtaExchangeResponseV1(
                        DiagnosisKeySubmissionToken.of("sub-token"),
                        Positive,
                        TestEndDate.of(2020, 4, 23),
                        LAB_RESULT
                    )
                )
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V1))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson(
                """
                { 
                    "testEndDate":"2020-04-23T00:00:00Z", 
                    "testResult":"POSITIVE", 
                    "diagnosisKeySubmissionToken":"sub-token",
                    "testKit":"LAB_RESULT"
                }
                """
            )
        }

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV1(CtaExchangeRequestV1(CtaToken.of("cc8f0b6z")), any(), any())
        }

        expectThat(events).contains(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange cta token handling test result not available yet`() {
        val virology = mockk<VirologyService> {
            every { exchangeCtaTokenForV1(any(), any(), any()) } returns CtaExchangeResult.Pending
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V1))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NO_CONTENT)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV1(CtaExchangeRequestV1(CtaToken.of("cc8f0b6z")), any(), any())
        }

        expectThat(events).contains(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange cta token handling cta token not found`() {
        val virology = mockk<VirologyService> {
            val request = slot<CtaExchangeRequestV1>()
            every {
                exchangeCtaTokenForV1(capture(request), any(), any())
            } answers {
                NotFound(request.captured.ctaToken)
            }
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V1))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NOT_FOUND)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV1(CtaExchangeRequestV1(CtaToken.of("cc8f0b6z")), any(), any())
        }
    }

    @Test
    fun `exchange cta token handling invalid cta token`() {
        val virology = mockk<VirologyService>()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson("""{"ctaToken":"invalid-cta-token"}""")

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(BAD_REQUEST)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(VirologyCtaExchange::class, UnprocessableVirologyCtaExchange::class)
    }

    @Test
    fun `lookup v2 maps test kits to json correctly with confirmatory day limit`() {
        val lookup = mockk<VirologyLookupService> {
            every { lookup(any(), any()) } returns VirologyLookupResult(
                testKit = RAPID_RESULT,
                confirmatoryTest = true,
                confirmatoryDayLimit = 2
            )
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/v2/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V2))

        val response = VirologySubmissionHandler(lookup = lookup).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson(
                """
                { 
                    "testEndDate":"2020-04-23T00:00:00Z", 
                    "testResult":"POSITIVE",
                    "testKit":"${RAPID_RESULT.name}",
                    "diagnosisKeySubmissionSupported": true,
                    "requiresConfirmatoryTest": true,
                    "confirmatoryDayLimit": 2,
                    "venueHistorySharingSupported": false,
                    "shouldOfferFollowUpTest": false
                }
                """
            )
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup v2 maps test kits to json correctly`(testKit: TestKit) {
        val lookup = mockk<VirologyLookupService> {
            every { lookup(any(), any()) } returns VirologyLookupResult(testKit)
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/v2/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V2))

        val response = VirologySubmissionHandler(lookup = lookup).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson(
                """
                { 
                    "testEndDate":"2020-04-23T00:00:00Z", 
                    "testResult":"POSITIVE",
                    "testKit":"${testKit.name}",
                    "diagnosisKeySubmissionSupported": true,
                    "requiresConfirmatoryTest": false,
                    "confirmatoryDayLimit": null,
                    "venueHistorySharingSupported": false,
                    "shouldOfferFollowUpTest": false
                }
                """
            )
        }
    }

    @Test
    fun `lookup v2 parses mobile version header`() {
        val lookup = mockk<VirologyLookupService> {
            every { lookup(any(), any()) } returns VirologyLookupResult()
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent", "p=Android,o=29,v=4.3.5,b=138")
            .withPath("/virology-test/v2/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V2))

        val response = VirologySubmissionHandler(lookup = lookup).handleRequest(requestEvent, aContext())

        expectThat(response).status.isSameAs(OK)

        verify { lookup.lookup(any(), MobileAppVersion.Version(4, 3, 5)) }
    }

    @Test
    fun `lookup v2 parses mobile version header handling unknown user agent`() {
        val lookup = mockk<VirologyLookupService> {
            every { lookup(any(), any()) } returns VirologyLookupResult()
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent-Dodgy", "null")
            .withPath("/virology-test/v2/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V2))

        val response = VirologySubmissionHandler(lookup = lookup).handleRequest(requestEvent, aContext())

        expectThat(response).status.isSameAs(OK)

        verify { lookup.lookup(any(), Unknown) }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `exchange v2 cta token for available rapid test result`(testKit: TestKit) {
        val virology = mockk<VirologyService> {
            every { exchangeCtaTokenForV2(any(), any(), any()) } returns
                CtaExchangeResult(
                    testKit = testKit,
                    submissionSupported = false,
                    confirmatoryTest = true
                )
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson(
                """ 
                { 
                    "testEndDate":"2020-04-23T00:00:00Z", 
                    "testResult":"POSITIVE", 
                    "diagnosisKeySubmissionToken":"sub-token",
                    "testKit":"${testKit.name}",
                    "diagnosisKeySubmissionSupported": false,
                    "requiresConfirmatoryTest": true,
                    "confirmatoryDayLimit": null,
                    "venueHistorySharingSupported": false,
                    "shouldOfferFollowUpTest": false
                }"""
            )
        }

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV2(CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country), any(), any())
        }

        expectThat(events).contains(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange v2 cta token for available rapid test result with confirmatory day limit`() {
        val virology = mockk<VirologyService> {
            every { exchangeCtaTokenForV2(any(), any(), any()) } returns
                CtaExchangeResult(
                    testKit = RAPID_RESULT,
                    confirmatoryTest = true,
                    confirmatoryDayLimit = 1
                )
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.isEqualToJson(
                """ 
                { 
                    "testEndDate":"2020-04-23T00:00:00Z", 
                    "testResult":"POSITIVE", 
                    "diagnosisKeySubmissionToken":"sub-token",
                    "testKit":"${RAPID_RESULT.name}",
                    "diagnosisKeySubmissionSupported": true,
                    "requiresConfirmatoryTest": true,
                    "confirmatoryDayLimit": 1,
                    "venueHistorySharingSupported": false,
                    "shouldOfferFollowUpTest": false
                }"""
            )
        }

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV2(CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country), any(), any())
        }

        expectThat(events).contains(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange v2 parses mobile version header`() {
        val virology = mockk<VirologyService> {
            every { exchangeCtaTokenForV2(any(), any(), any()) } returns CtaExchangeResult()
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent", "p=Android,o=29,v=4.3.5,b=138")
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response).status.isSameAs(OK)

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV2(
                CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country),
                MobileAppVersion.Version(4, 3, 5),
                MobileOS.Android
            )
        }
    }

    @Test
    fun `exchange v2 parses mobile version header handling unknown user agent`() {
        val virology = mockk<VirologyService> {
            every { exchangeCtaTokenForV2(any(), any(), any()) } returns CtaExchangeResult()
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent-Dodgy", "null")
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response).status.isSameAs(OK)

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV2(
                CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country), any(), any()
            )
        }
    }

    @Test
    fun `exchange v2 cta token handling cta token not found`() {
        val virology = mockk<VirologyService> {
            val request = slot<CtaExchangeRequestV2>()
            every {
                exchangeCtaTokenForV2(capture(request), any(), any())
            } answers {
                NotFound(request.captured.ctaToken)
            }
        }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent", "p=Android,o=29,v=4.3.5,b=138")
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NOT_FOUND)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        verify(exactly = 1) {
            virology.exchangeCtaTokenForV2(
                CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country), any(), any()
            )
        }
    }

    @Test
    fun `polling v1 test result returns pending when lab uploads positive RAPID_RESULT result via v2`() {
        every { persistence.getTestResult(any()) } returns TestData.positiveRapidResult
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns true

        val virology = VirologyService()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))

        val response = VirologySubmissionHandler(virology).handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NO_CONTENT)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }

        expectThat(events).contains(VirologyResults::class)
    }

    private fun VirologyService(persistenceService: VirologyPersistenceService = persistence) =
        VirologyService(persistenceService, tokenGenerator, CLOCK, virologyPolicyConfig, events)

    private fun VirologyLookupService(persistenceService: VirologyPersistenceService = persistence) =
        VirologyLookupService(persistenceService, CLOCK, virologyPolicyConfig, events)

    private fun lookupPayload(apiVersion: ApiVersion) = when (apiVersion) {
        V1 -> """{"testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7"}"""
        V2 -> """{"testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7","country": "England"}"""
    }

    private fun ctaExchangePayload(apiVersion: ApiVersion) = when (apiVersion) {
        V1 -> """{"ctaToken":"cc8f0b6z"}"""
        V2 -> """{"ctaToken":"cc8f0b6z", "country": "England"}"""
    }

    private fun VirologyLookupResult(
        testKit: TestKit = LAB_RESULT,
        submissionSupported: Boolean = true,
        confirmatoryTest: Boolean = false,
        confirmatoryDayLimit: Int? = null,
        shouldOfferFollowUpTest: Boolean = false
    ) = VirologyLookupResult.AvailableV2(
        VirologyLookupResponseV2(
            testEndDate = TestEndDate.of(2020, 4, 23),
            testResult = Positive,
            testKit = testKit,
            diagnosisKeySubmissionSupported = submissionSupported,
            requiresConfirmatoryTest = confirmatoryTest,
            confirmatoryDayLimit = confirmatoryDayLimit,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest
        )
    )

    private fun CtaExchangeResult(
        testKit: TestKit = LAB_RESULT,
        submissionSupported: Boolean = true,
        confirmatoryTest: Boolean = false,
        confirmatoryDayLimit: Int? = null,
        shouldOfferFollowUpTest: Boolean = false
    ) = CtaExchangeResult.AvailableV2(
        CtaExchangeResponseV2(
            diagnosisKeySubmissionToken = DiagnosisKeySubmissionToken.of("sub-token"),
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 4, 23),
            testKit = testKit,
            diagnosisKeySubmissionSupported = submissionSupported,
            requiresConfirmatoryTest = confirmatoryTest,
            confirmatoryDayLimit = confirmatoryDayLimit,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest
        )
    )

    private fun VirologySubmissionHandler(
        virology: VirologyService = VirologyService(persistence),
        lookup: VirologyLookupService = VirologyLookupService(persistence),
        env: Environment = environment
    ) = VirologySubmissionHandler(
        environment = env,
        clock = CLOCK,
        delayDuration = throttleDuration,
        events = events,
        mobileAuthenticator = authenticator,
        signer = signer,
        persistence = persistence,
        virology = virology,
        virologyLookup = lookup,
        websiteConfig = websiteConfig
    ) { true }

    private enum class ApiVersion { V1, V2 }
}
