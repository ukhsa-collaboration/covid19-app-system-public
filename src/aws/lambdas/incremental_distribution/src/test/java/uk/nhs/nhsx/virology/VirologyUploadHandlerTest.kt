package uk.nhs.nhsx.virology

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.natpryce.snodge.json.defaultJsonMutagens
import com.natpryce.snodge.json.forStrings
import com.natpryce.snodge.mutants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import uk.nhs.nhsx.ContextBuilder
import uk.nhs.nhsx.ProxyRequestBuilder
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasBody
import uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasStatus
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import kotlin.random.Random

class VirologyUploadHandlerTest {

    private val payloadJson = """{
        "ctaToken": "cc8f0b6z",
        "testEndDate": "2020-04-23T00:00:00Z",
        "testResult": "NEGATIVE"
    }"""

    private val npexPath = "/upload/virology-test/npex-result"
    private val correctToken = "anything"

    private val environment = TestEnvironments.TEST.apply(mapOf("MAINTENANCE_MODE" to "false"))

    @Test
    fun `accepts test result returns 202`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.ACCEPTED_202))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("successfully processed")))
    }

    @Test
    fun `test order is not present returns 400`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.OrderNotFound()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(CoreMatchers.nullValue(String::class.java)))
    }

    @Test
    fun `transaction failure returns 409`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.TransactionFailed("")
        }
        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.CONFLICT_409))
        assertThat(responseEvent, hasBody(CoreMatchers.nullValue(String::class.java)))
    }

    @Test
    fun `invalid path returns 404`() {
        val uploadService = Mockito.mock(VirologyService::class.java)
        val handler = VirologyUploadHandler(environment, { true }, uploadService)
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/incorrect/npex-result")
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo(null)))
    }

    @Test
    fun `invalid method returns 405`() {
        val uploadService = Mockito.mock(VirologyService::class.java)
        val handler = VirologyUploadHandler(environment, { true }, uploadService)
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo(null)))
    }

    @Test
    fun `empty body returns 400`() {
        val uploadService = Mockito.mock(VirologyService::class.java)
        val handler = VirologyUploadHandler(environment, { true }, uploadService)
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson("")
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo(null)))
    }

    @Test
    fun `random payload does not cause 500`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)
        val originalJson = payloadJson

        Random.mutants(defaultJsonMutagens().forStrings(), 100, originalJson)
            .forEach { json: String ->
                if (json != originalJson) {
                    val request = requestEventWithPayload(json)
                    val response = handler.handleRequest(request, ContextBuilder.aContext())
                    assertThat(response, CoreMatchers.not(hasStatus(HttpStatusCode.INTERNAL_SERVER_ERROR_500)))
                }
            }
    }

    @Test
    fun `accepts english token gen request`() {
        val service = mockk<VirologyService>(relaxed = true)
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse.of("cta-123")

        val path = "/upload/virology-test/eng-result-tokengen"
        val responseEvent = sendTokenGenRequestTo(path, service)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("POSITIVE", "2020-09-07T01:01:01Z")
            )
        }

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("""{"ctaToken":"cta-123"}""")))
    }

    @Test
    fun `accepts welsh token gen request`() {
        val uploadService = mockk<VirologyService>(relaxed = true)
        every { uploadService.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse.of("cta-123")

        val path = "/upload/virology-test/wls-result-tokengen"
        val responseEvent = sendTokenGenRequestTo(path, uploadService)

        verify(exactly = 1) {
            uploadService.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("POSITIVE", "2020-09-07T01:01:01Z")
            )
        }

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("""{"ctaToken":"cta-123"}""")))
    }

    @Test
    fun `accepts self administered token gen request`() {
        val uploadService = mockk<VirologyService>(relaxed = true)
        every { uploadService.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse.of("cta-123")

        val path = "/upload/virology-test/lfd-result-tokengen"
        val responseEvent = sendTokenGenRequestTo(path, uploadService)

        verify(exactly = 1) {
            uploadService.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("POSITIVE", "2020-09-07T01:01:01Z")
            )
        }

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("""{"ctaToken":"cta-123"}""")))
    }

    private fun sendTokenGenRequestTo(path: String, uploadService: VirologyService): APIGatewayProxyResponseEvent {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(path)
            .withJson(""" 
                {
                    "testEndDate": "2020-09-07T01:01:01Z",
                    "testResult": "POSITIVE"
                } 
                """)
            .withBearerToken(correctToken)
            .build()

        val handler = VirologyUploadHandler(environment, { true }, uploadService)
        return handler.handleRequest(requestEvent, ContextBuilder.aContext())
    }

    private fun requestEventWithPayload(requestPayload: String): APIGatewayProxyRequestEvent {
        return ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(requestPayload)
            .build()
    }
}