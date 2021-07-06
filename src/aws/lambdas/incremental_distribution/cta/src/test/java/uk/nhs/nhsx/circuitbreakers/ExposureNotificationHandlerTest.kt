package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec.ECDSA_SHA_256
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Json.readJsonOrNull
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.Parameter
import uk.nhs.nhsx.core.events.CircuitBreakerExposureRequest
import uk.nhs.nhsx.core.events.CircuitBreakerExposureResolution
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.build
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId
import java.util.*

class ExposureNotificationHandlerTest {

    private val contentSigner = Signer {
        Signature(KeyId.of("some-id"), ECDSA_SHA_256, "TEST_SIGNATURE".toByteArray())
    }

    private val events = RecordingEvents()

    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner), events)

    private val initial = Parameter { ApprovalStatus.PENDING }
    private val poll = Parameter { ApprovalStatus.YES }

    private val breaker = CircuitBreakerService(initial, poll)
    private val handler = ExposureNotificationHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        SystemClock.CLOCK,
        events,
        { true },
        AwsSsmParameters(),
        signer,
        breaker,
        { true }
    )

    @Test
    fun `handle circuit breaker request success`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson(
                """
                {
                    "matchedKeyCount": 2,
                    "daysSinceLastExposure": 3,
                    "maximumRiskScore": 150.123456
                }
                """.trimIndent()
            )

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val tokenResponse = readJsonOrNull<TokenResponse>(response.body) ?: error("")
        assertThat(tokenResponse.approval).matches("pending")
        assertThat(tokenResponse.approvalToken).matches("[a-zA-Z0-9]+")
        events.contains(CircuitBreakerExposureRequest::class)

    }

    @Test
    fun `handle circuit breaker request success with extra risk calculation score field`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson(
                """
                {
                    "matchedKeyCount": 2,
                    "daysSinceLastExposure": 3,
                    "maximumRiskScore": 150.123456,
                    "riskCalculationVersion": 8
                }
                """.trimIndent()
            )

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val tokenResponse = readJsonOrNull<TokenResponse>(response.body) ?: error("")
        assertThat(tokenResponse.approval).matches("pending")
        assertThat(tokenResponse.approvalToken).matches("[a-zA-Z0-9]+")
        events.contains(CircuitBreakerExposureRequest::class)
    }

    @Test
    fun `handle circuit breaker request invalid json data`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson("{\"invalidField\": null}")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle circuit breaker request invalid json format`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson("{ invalid }")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle circuit breaker request no body`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle circuit breaker no such path`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/unknown-feature")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(404)
    }

    @Test
    fun `handle circuit breaker missing token`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/resolution")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
    }

    @Test
    fun `handle circuit breaker resolution success`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/resolution/abc123")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val resolutionResponse = readJsonOrNull<ResolutionResponse>(response.body) ?: error("")
        assertThat(resolutionResponse.approval).matches(ApprovalStatus.YES.statusName)
        events.contains(CircuitBreakerExposureResolution::class)
    }

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent) =
        Optional.ofNullable(response.headers).orElse(emptyMap())
}
