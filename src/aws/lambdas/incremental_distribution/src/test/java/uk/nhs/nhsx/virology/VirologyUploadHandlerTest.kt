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
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenRequestV1
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenRequestV2
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasBody
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_RESULT
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import kotlin.random.Random

class VirologyUploadHandlerTest {

    private val payloadJsonV1 = """{
        "ctaToken": "cc8f0b6z",
        "testEndDate": "2020-04-23T00:00:00Z",
        "testResult": "NEGATIVE"
    }"""

    private val payloadJsonV2 = """{
        "ctaToken": "cc8f0b6z",
        "testEndDate": "2020-04-23T00:00:00Z",
        "testResult": "POSITIVE",
        "testKit": "RAPID_RESULT"
    }"""

    private val npexPath = "/upload/virology-test/npex-result"
    private val npexV2Path = "/upload/virology-test/v2/npex-result"
    private val correctToken = "anything"

    private val environment = TestEnvironments.TEST.apply(mapOf(
        "MAINTENANCE_MODE" to "false",
        "virology_v2_apis_enabled" to "true",
    ))

    @Test
    fun `accepts npex test result v1 returns 202`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV1)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
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
            .withJson(payloadJsonV1)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
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
            .withJson(payloadJsonV1)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
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
            .withJson(payloadJsonV1)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, aContext())
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
            .withJson(payloadJsonV1)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, aContext())
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
        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo(null)))
    }

    @Test
    fun `random payload does not cause 500`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)
        val originalJson = payloadJsonV1

        Random.mutants(defaultJsonMutagens().forStrings(), 100, originalJson)
            .forEach { json: String ->
                if (json != originalJson) {
                    val request = requestEventWithPayload(json)
                    val response = handler.handleRequest(request, aContext())
                    assertThat(response, CoreMatchers.not(hasStatus(HttpStatusCode.INTERNAL_SERVER_ERROR_500)))
                }
            }
    }

    @Test
    fun `accepts english token gen request`() {
        val service = mockk<VirologyService>(relaxed = true)
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse.of("cta-123")

        val path = "/upload/virology-test/eng-result-tokengen"
        val responseEvent = sendTokenGenRequestToV1(path, service)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("POSITIVE", "2020-09-07T01:01:01Z", LAB_RESULT)
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
        val responseEvent = sendTokenGenRequestToV1(path, uploadService)

        verify(exactly = 1) {
            uploadService.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("POSITIVE", "2020-09-07T01:01:01Z", LAB_RESULT)
            )
        }

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("""{"ctaToken":"cta-123"}""")))
    }

    @Test
    fun `accepts test result v1 returns 422 on invalid testKit`() {
        val service = mockk<VirologyService> {}
        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV2)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422))
    }

    @Test
    fun `accepts npex test result v2 returns 202`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexV2Path)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV2)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.ACCEPTED_202))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("successfully processed")))
    }

    @Test
    fun `accepts npex test result v2 returns 404 by default`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }

        val environment = TestEnvironments.TEST.apply(mapOf(
            "MAINTENANCE_MODE" to "false",
        ))

        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexV2Path)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV2)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
    }

    @Test
    fun `accepts test result v2 returns 422 on invalid testType`() {
        val service = mockk<VirologyService> {}
        val handler = VirologyUploadHandler(environment, { true }, service)

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexV2Path)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV1)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422))
    }

    @Test
    fun `accepts fiorano test result v1 returns 202`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)
        val path = "/upload/virology-test/fiorano-result"

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(path)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV1)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.ACCEPTED_202))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("successfully processed")))
    }

    @Test
    fun `accepts fiorano test result v2 returns 202`() {
        val service = mockk<VirologyService> {
            every { acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()
        }
        val handler = VirologyUploadHandler(environment, { true }, service)
        val path = "/upload/virology-test/v2/fiorano-result"

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(path)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV2)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.ACCEPTED_202))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("successfully processed")))
    }

    @Test
    fun `accepts fiorano test result v2 returns 404 by default`() {
        val service = mockk<VirologyService> {}
        val environment = TestEnvironments.TEST.apply(mapOf(
            "MAINTENANCE_MODE" to "false",
        ))
        val handler = VirologyUploadHandler(environment, { true }, service)
        val path = "/upload/virology-test/v2/fiorano-result"

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(path)
            .withBearerToken(correctToken)
            .withJson(payloadJsonV2)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
    }

    @Test
    fun `accepts v2 english token gen request`() {
        val service = mockk<VirologyService>(relaxed = true)
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse.of("cta-123")

        val path = "/upload/virology-test/v2/eng-result-tokengen"
        val responseEvent = sendTokenGenRequestToV2(path, service)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("POSITIVE", "2020-09-07T01:01:01Z", RAPID_RESULT)
            )
        }

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("""{"ctaToken":"cta-123"}""")))
    }

    @Test
    fun `accepts v2 english token gen request and returns 404 by default`() {
        val service = mockk<VirologyService>{}
        val environment = TestEnvironments.TEST.apply(mapOf(
            "MAINTENANCE_MODE" to "false",
        ))
        val path = "/upload/virology-test/v2/eng-result-tokengen"
        val responseEvent = sendTokenGenRequestToV2(path, service, environment)

        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
    }

    @Test
    fun `accepts v2 welsh token gen request`() {
        val uploadService = mockk<VirologyService>(relaxed = true)
        every { uploadService.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse.of("cta-123")

        val path = "/upload/virology-test/v2/wls-result-tokengen"
        val responseEvent = sendTokenGenRequestToV2(path, uploadService)

        verify(exactly = 1) {
            uploadService.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequest("POSITIVE", "2020-09-07T01:01:01Z", RAPID_RESULT)
            )
        }

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(CoreMatchers.equalTo("""{"ctaToken":"cta-123"}""")))
    }

    @Test
    fun `accepts v2 welsh token gen request and returns 404 by default`() {
        val uploadService = mockk<VirologyService>(relaxed = true)
        val environment = TestEnvironments.TEST.apply(mapOf(
            "MAINTENANCE_MODE" to "false",
        ))
        val path = "/upload/virology-test/v2/wls-result-tokengen"
        val responseEvent = sendTokenGenRequestToV2(path, uploadService, environment)

        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
    }

    private fun sendTokenGenRequestToV1(path: String, uploadService: VirologyService, env: Environment = environment): APIGatewayProxyResponseEvent =
        sendTokenGenRequestTo(path, tokenGenRequestV1, uploadService, env)

    private fun sendTokenGenRequestToV2(path: String, uploadService: VirologyService, env: Environment = environment): APIGatewayProxyResponseEvent =
        sendTokenGenRequestTo(path, tokenGenRequestV2, uploadService, env)

    private fun sendTokenGenRequestTo(
        path: String,
        payload: String,
        uploadService: VirologyService,
        environment: Environment,
    ): APIGatewayProxyResponseEvent {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(path)
            .withJson(payload)
            .withBearerToken(correctToken)
            .build()

        val handler = VirologyUploadHandler(environment, { true }, uploadService)
        return handler.handleRequest(requestEvent, aContext())
    }

    private fun requestEventWithPayload(requestPayload: String) =
        ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(npexPath)
            .withBearerToken(correctToken)
            .withJson(requestPayload)
            .build()
}
