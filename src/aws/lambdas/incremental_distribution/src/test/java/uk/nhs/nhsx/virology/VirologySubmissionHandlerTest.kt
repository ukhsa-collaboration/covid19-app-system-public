package uk.nhs.nhsx.virology

import com.amazonaws.HttpMethod
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONParser
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.events.VirologyCtaExchange
import uk.nhs.nhsx.core.events.VirologyOrder
import uk.nhs.nhsx.core.events.VirologyRegister
import uk.nhs.nhsx.core.events.VirologyResults
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.VirologySubmissionHandlerTest.ApiVersion.V1
import uk.nhs.nhsx.virology.VirologySubmissionHandlerTest.ApiVersion.V2
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequest
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponse
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.result.VirologyLookupResult
import java.time.Duration
import java.util.*

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
    private val environment = TestEnvironments.TEST.apply(
        mapOf(
            "MAINTENANCE_MODE" to "false",
            "custom_oai" to "OAI",
        )
    )
    private val persistenceService = mockk<VirologyPersistenceService>()
    private val tokenGenerator = mockk<TokensGenerator>()
    private val virologyPolicyConfig = mockk<VirologyPolicyConfig>()
    private val country = Country.of("England")

    @Test
    fun `handle test result request success`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.positiveLabResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        assertEquals(
            """{"testEndDate":"2020-04-23T18:34:03Z","testResult":"POSITIVE","testKit":"LAB_RESULT"}""",
            response.body,
            JSONCompareMode.STRICT
        )
        events.containsExactly(VirologyResults::class)
    }

    @Test
    fun `handle pending test result request success no content`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyResults::class)
    }

    @Test
    fun `handle test result request missing token`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("""{"testResultPollingToken":""}""")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `handle test result request null token`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("""{"testResultPollingToken":null}""")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `handle test result request that does not exist`() {
        every { persistenceService.getTestResult(any()) } returns Optional.empty()

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.body).isEqualTo("Test result lookup submitted for unknown testResultPollingToken")
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyResults::class)
    }

    @Test
    fun `handle test result request for incorrect request json`() {
        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("""{"invalidField":"98cff3dd-882c-417b-a00a-350a205378c7"}""")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `handle test result request for missing body`() {
        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyResults::class, UnprocessableJson::class)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/virology-test/home-kit/order", "/virology-test/v2/order"])
    fun `handle test order request success`(orderUrl: String) {
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            "cc8f0b6z", "polling-token", "submission-token"
        )

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath(orderUrl)
            .withBearerToken("anything")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val jsonObject = JSONParser.parseJSON(response.body) as JSONObject
        assertThat(jsonObject["diagnosisKeySubmissionToken"] as String).isEqualTo("submission-token")
        assertThat(jsonObject["testResultPollingToken"] as String).isEqualTo("polling-token")
        assertThat(jsonObject["tokenParameterValue"] as String).isEqualTo("cc8f0b6z")
        assertThat(jsonObject["websiteUrlWithQuery"] as String).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")
        events.contains(VirologyOrder::class)
    }

    @Test
    fun `handle test register request success`() {
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            "cc8f0b6z", "polling-token", "submission-token"
        )

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/home-kit/register")
            .withBearerToken("anything")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val jsonObject = JSONParser.parseJSON(response.body) as JSONObject
        assertThat(jsonObject["diagnosisKeySubmissionToken"] as String).isEqualTo("submission-token")
        assertThat(jsonObject["testResultPollingToken"] as String).isEqualTo("polling-token")
        assertThat(jsonObject["tokenParameterValue"] as String).isEqualTo("cc8f0b6z")
        assertThat(jsonObject["websiteUrlWithQuery"] as String).isEqualTo("https://example.register-a-test.uk?ctaToken=cc8f0b6z")
        events.contains(VirologyRegister::class)
    }

    @Test
    fun `handle unknown path`() {
        val service = virologyService(mockk())

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/unknown/path")
            .withBearerToken("anything")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `exchange invalid cta token`() {
        val service = mockk<VirologyService>()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(""" "{"ctaToken":null} """)
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(400)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `exchange cta token for available test result`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaTokenForV1(any()) } returns
                CtaExchangeResult.Available(
                    CtaExchangeResponse("sub-token", "POSITIVE", "2020-04-23T18:34:03Z", LAB_RESULT)
                )
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V1))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        val expectedResponse =
            """ { 
                |"testEndDate":"2020-04-23T18:34:03Z", 
                |"testResult":"POSITIVE", 
                |"diagnosisKeySubmissionToken":"sub-token",
                |"testKit":"LAB_RESULT"
            |} """.trimMargin()

        assertEquals(expectedResponse, response.body, JSONCompareMode.STRICT)

        verify(exactly = 1) { service.exchangeCtaTokenForV1(CtaExchangeRequest(CtaToken.of("cc8f0b6z"))) }
        events.containsExactly(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange cta token handling test result not available yet`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaTokenForV1(any()) } returns CtaExchangeResult.Pending()
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V1))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        verify(exactly = 1) { service.exchangeCtaTokenForV1(CtaExchangeRequest(CtaToken.of("cc8f0b6z"))) }
        events.containsExactly(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange cta token handling cta token not found`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaTokenForV1(any()) } returns CtaExchangeResult.NotFound()
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V1))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        verify(exactly = 1) { service.exchangeCtaTokenForV1(CtaExchangeRequest(CtaToken.of("cc8f0b6z"))) }
        events.containsExactly(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange cta token handling invalid cta token`() {
        val service = mockk<VirologyService>()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson("""{"ctaToken":"invalid-cta-token"}""")
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(400)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyCtaExchange::class, UnprocessableVirologyCtaExchange::class)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup v2 maps test kits to json correctly`(testKit: TestKit) {
        val service = mockk<VirologyService> {
            every { virologyLookupForV2(any(), any()) } returns lookupAvailableResultV2(testKit)
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/v2/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V2))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val expectedResponse =
            """ { 
                |"testEndDate":"2020-04-23T18:34:03Z", 
                |"testResult":"POSITIVE",
                |"testKit":"${testKit.name}",
                |"diagnosisKeySubmissionSupported": true,
                |"requiresConfirmatoryTest": false
            |} """.trimMargin()

        assertEquals(expectedResponse, response.body, JSONCompareMode.STRICT)
    }

    @Test
    fun `lookup v2 parses mobile version header`() {
        val service = mockk<VirologyService> {
            every { virologyLookupForV2(any(), any()) } returns lookupAvailableResultV2()
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent", "p=Android,o=29,v=4.3.5,b=138")
            .withPath("/virology-test/v2/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V2))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(200)
        verify { service.virologyLookupForV2(any(), MobileAppVersion.Version(4, 3, 5)) }
    }

    @Test
    fun `lookup v2 parses mobile version header handling unknown user agent`() {
        val service = mockk<VirologyService> {
            every { virologyLookupForV2(any(), any()) } returns lookupAvailableResultV2()
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent-Dodgy", "null")
            .withPath("/virology-test/v2/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V2))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(200)
        verify { service.virologyLookupForV2(any(), MobileAppVersion.Unknown) }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `exchange v2 cta token for available rapid test result`(testKit: TestKit) {
        val service = mockk<VirologyService> {
            every { exchangeCtaTokenForV2(any(), any()) } returns
                ctaExchangeAvailableResultV2(testKit, submissionSupported = false, confirmatoryTest = true)
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        val expectedResponse =
            """ { 
                |"testEndDate":"2020-04-23T18:34:03Z", 
                |"testResult":"POSITIVE", 
                |"diagnosisKeySubmissionToken":"sub-token",
                |"testKit":${testKit.name},
                |"diagnosisKeySubmissionSupported": false,
                |"requiresConfirmatoryTest": true
            |} """.trimMargin()

        assertEquals(expectedResponse, response.body, JSONCompareMode.STRICT)

        verify(exactly = 1) {
            service.exchangeCtaTokenForV2(CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country), any())
        }
        events.containsExactly(VirologyCtaExchange::class)
    }

    @Test
    fun `exchange v2 parses mobile version header`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaTokenForV2(any(), any()) } returns ctaExchangeAvailableResultV2()
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent", "p=Android,o=29,v=4.3.5,b=138")
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(200)
        verify(exactly = 1) {
            service.exchangeCtaTokenForV2(
                CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country), any()
            )
        }
    }

    @Test
    fun `exchange v2 parses mobile version header handling unknown user agent`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaTokenForV2(any(), any()) } returns ctaExchangeAvailableResultV2()
        }

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withHeader("User-Agent-Dodgy", "null")
            .withPath("/virology-test/v2/cta-exchange")
            .withBearerToken("anything")
            .withJson(ctaExchangePayload(V2))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(200)
        verify(exactly = 1) {
            service.exchangeCtaTokenForV2(
                CtaExchangeRequestV2(CtaToken.of("cc8f0b6z"), country), any()
            )
        }
    }

    @Test
    fun `polling v1 test result returns pending when lab uploads positive RAPID_RESULT result via v2`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.positiveRapidResult)
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns true

        val service = virologyService()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson(lookupPayload(V1))
            .build()

        val response = newHandler(service).handleRequest(requestEvent, aContext())

        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        events.containsExactly(VirologyResults::class)
    }

    private fun virologyService(persistence: VirologyPersistenceService = persistenceService) =
        VirologyService(persistence, tokenGenerator, CLOCK, virologyPolicyConfig, RecordingEvents())

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent): Map<String, String> {
        return Optional.ofNullable(response.headers).orElse(emptyMap())
    }

    private fun lookupPayload(apiVersion: ApiVersion) = when (apiVersion) {
        V1 -> """{"testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7"}"""
        V2 ->
            """{
                "testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7",
                "country": "England"
            }"""
    }

    private fun ctaExchangePayload(apiVersion: ApiVersion): String = when (apiVersion) {
        V1 -> """{"ctaToken":"cc8f0b6z"}"""
        V2 ->
            """ {
                "ctaToken":"cc8f0b6z",
                "country": "England"
            }"""
    }

    private fun lookupAvailableResultV2(
        testKit: TestKit = LAB_RESULT,
        submissionSupported: Boolean = true,
        confirmatoryTest: Boolean = false
    ) =
        VirologyLookupResult.AvailableV2(
            VirologyLookupResponseV2(
                "2020-04-23T18:34:03Z", "POSITIVE", testKit, submissionSupported, confirmatoryTest
            )
        )

    private fun ctaExchangeAvailableResultV2(
        testKit: TestKit = LAB_RESULT,
        submissionSupported: Boolean = true,
        confirmatoryTest: Boolean = false
    ) =
        CtaExchangeResult.AvailableV2(
            CtaExchangeResponseV2(
                "sub-token", "POSITIVE", "2020-04-23T18:34:03Z", testKit, submissionSupported, confirmatoryTest
            )
        )

    private fun newHandler(service: VirologyService = mockk {}, env: Environment = environment) =
        VirologySubmissionHandler(
            env,
            CLOCK,
            throttleDuration,
            events,
            authenticator,
            signer,
            service,
            websiteConfig,
            { true }
        )

    private enum class ApiVersion { V1, V2 }
}
