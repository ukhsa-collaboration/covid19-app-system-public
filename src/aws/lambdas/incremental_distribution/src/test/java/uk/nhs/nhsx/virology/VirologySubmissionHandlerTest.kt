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
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONParser
import uk.nhs.nhsx.ContextBuilder
import uk.nhs.nhsx.ProxyRequestBuilder
import uk.nhs.nhsx.TestData
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequest
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponse
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyDynamoService
import java.time.Duration
import java.util.Optional

class VirologySubmissionHandlerTest {

    private val contentSigner = mockk<Signer> {
        every { sign(any()) } returns
            Signature(
                KeyId.of("TEST_KEY_ID"),
                SigningAlgorithmSpec.ECDSA_SHA_256, "TEST_SIGNATURE".toByteArray()
            )
    }
    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner))
    private val websiteConfig = VirologyWebsiteConfig("https://example.order-a-test.uk", "https://example.register-a-test.uk")
    private val throttleDuration = Duration.ofMillis(1)
    private val authenticator = Authenticator { true }

    private val environment = TestEnvironments.TEST.apply(mapOf("MAINTENANCE_MODE" to "false"))

    @Test
    fun `handle test result request success`() {
        val persistenceService = mockk<VirologyDynamoService>()
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.positiveTestResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val handler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        JSONAssert.assertEquals("{\"testEndDate\":\"2020-04-23T18:34:03Z\",\"testResult\":\"POSITIVE\"}", response.body, JSONCompareMode.STRICT)
    }

    @Test
    fun `handle test result request success no content`() {
        val persistenceService = mockk<VirologyDynamoService>()
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle test result request missing token`() {
        val persistenceService = mockk<VirologyDynamoService>()
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"\"}")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle test result request null token`() {
        val persistenceService = mockk<VirologyDynamoService>()
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs

        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":null}")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle test result request that does not exist`() {
        val persistenceService = mockk<VirologyDynamoService>()
        every { persistenceService.getTestResult(any()) } returns Optional.empty()

        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.body).isEqualTo("Test result lookup submitted for unknown testResultPollingToken")
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle test result request for incorrect request json`() {
        val persistenceService = mockk<VirologyDynamoService>()

        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"invalidField\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle test result request for missing body`() {
        val persistenceService = mockk<VirologyDynamoService>()

        val service = VirologyService(
            persistenceService,
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle test order request success`() {
        val persistenceService = mockk<VirologyDynamoService>()
        val tokenGenerator = mockk<TokensGenerator>()
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            "cc8f0b6z", "polling-token", "submission-token"
        )

        val service = VirologyService(
            persistenceService,
            tokenGenerator,
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/home-kit/order")
            .withBearerToken("anything")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val jsonObject = JSONParser.parseJSON(response.body) as JSONObject
        assertThat(jsonObject["diagnosisKeySubmissionToken"] as String).isEqualTo("submission-token")
        assertThat(jsonObject["testResultPollingToken"] as String).isEqualTo("polling-token")
        assertThat(jsonObject["tokenParameterValue"] as String).isEqualTo("cc8f0b6z")
        assertThat(jsonObject["websiteUrlWithQuery"] as String).isEqualTo("https://example.order-a-test.uk?ctaToken=cc8f0b6z")
    }

    @Test
    fun `handle test register request success`() {
        val persistenceService = mockk<VirologyDynamoService>()
        val tokenGenerator = mockk<TokensGenerator>()
        every { persistenceService.persistTestOrder(any(), any()) } returns TestOrder(
            "cc8f0b6z", "polling-token", "submission-token"
        )

        val service = VirologyService(
            persistenceService,
            tokenGenerator,
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/home-kit/register")
            .withBearerToken("anything")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val jsonObject = JSONParser.parseJSON(response.body) as JSONObject
        assertThat(jsonObject["diagnosisKeySubmissionToken"] as String).isEqualTo("submission-token")
        assertThat(jsonObject["testResultPollingToken"] as String).isEqualTo("polling-token")
        assertThat(jsonObject["tokenParameterValue"] as String).isEqualTo("cc8f0b6z")
        assertThat(jsonObject["websiteUrlWithQuery"] as String).isEqualTo("https://example.register-a-test.uk?ctaToken=cc8f0b6z")
    }

    @Test
    fun `handle unknown path`() {
        val service = VirologyService(
            mockk(),
            TokensGenerator(),
            SystemClock.CLOCK
        )

        val virologySubmissionHandler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, throttleDuration)
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/unknown/path")
            .withBearerToken("anything")
            .build()

        val response = virologySubmissionHandler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `exchange invalid cta token`() {
        val service = mockk<VirologyService>()

        val handler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, Duration.ofMillis(1))

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson(""" "{"ctaToken":null} """)
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(400)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `exchange cta token for available test result`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaToken(any()) } returns
                CtaExchangeResult.Available(
                    CtaExchangeResponse(
                        "sub-token", "POSITIVE", "2020-04-23T18:34:03Z"
                    )
                )
        }


        val handler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, Duration.ofMillis(1))

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson("{\"ctaToken\":\"cc8f0b6z\"}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
        val expectedResponse =
            """ { 
                |"testEndDate":"2020-04-23T18:34:03Z", 
                |"testResult":"POSITIVE", 
                |"diagnosisKeySubmissionToken":"sub-token"
            |} """.trimMargin()

        JSONAssert.assertEquals(expectedResponse, response.body, JSONCompareMode.STRICT)

        verify(exactly = 1) { service.exchangeCtaToken(CtaExchangeRequest(CtaToken.of("cc8f0b6z"))) }
    }

    @Test
    fun `exchange cta token handling test result not available yet`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaToken(any()) } returns CtaExchangeResult.Pending()
        }

        val handler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, Duration.ofMillis(1))

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson("{\"ctaToken\":\"cc8f0b6z\"}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        verify(exactly = 1) { service.exchangeCtaToken(CtaExchangeRequest(CtaToken.of("cc8f0b6z"))) }
    }

    @Test
    fun `exchange cta token handling cta token not found`() {
        val service = mockk<VirologyService> {
            every { exchangeCtaToken(any()) } returns CtaExchangeResult.NotFound()
        }

        val handler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, Duration.ofMillis(1))

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson("{\"ctaToken\":\"cc8f0b6z\"}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        verify(exactly = 1) { service.exchangeCtaToken(CtaExchangeRequest(CtaToken.of("cc8f0b6z"))) }
    }

    @Test
    fun `exchange cta token handling invalid cta token`() {
        val service = mockk<VirologyService>()

        val handler = VirologySubmissionHandler(environment, authenticator, signer, service, websiteConfig, Duration.ofMillis(1))

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/cta-exchange")
            .withBearerToken("anything")
            .withJson("{\"ctaToken\":\"invalid-cta-token\"}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(400)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

    }

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent): Map<String, String> {
        return Optional.ofNullable(response.headers).orElse(emptyMap())
    }
}