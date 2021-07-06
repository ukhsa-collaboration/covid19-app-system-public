package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.aws.ssm.Parameter
import uk.nhs.nhsx.core.events.CircuitBreakerVenueRequest
import uk.nhs.nhsx.core.events.CircuitBreakerVenueResolution
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.proxy
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId
import java.util.*

class RiskyVenueHandlerTest {

    private val contentSigner = Signer {
        Signature(
            KeyId.of("some-id"),
            SigningAlgorithmSpec.ECDSA_SHA_256,
            "TEST_SIGNATURE".toByteArray()
        )
    }

    private val events = RecordingEvents()

    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner), events)

    private val initial = Parameter { ApprovalStatus.PENDING }
    private val poll = Parameter { ApprovalStatus.YES }

    private val breaker = CircuitBreakerService(initial, poll)

    private val handler = RiskyVenueHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ), SystemClock.CLOCK, events, { true }, proxy(), signer, breaker, { true }
    )

    @Test
    fun `handle circuit breaker request with venue id`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")
            .withJson("""{"venueId": "MAX8CHR1"}""")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val tokenResponse = Json.readJsonOrNull<TokenResponse>(response.body) ?: error("")
        assertThat(tokenResponse.approval).matches("pending")
        assertThat(tokenResponse.approvalToken).matches("[a-zA-Z0-9]+")
        events.contains(CircuitBreakerVenueRequest::class)
    }

    @Test
    fun `handle circuit breaker request invalid json data`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")
            .withJson("""{"invalidField": null}""")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle circuit breaker request no body`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
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
            .withPath("/circuit-breaker/venue/resolution")
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
            .withPath("/circuit-breaker/venue/resolution/abc123")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val resolutionResponse = Json.readJsonOrNull<ResolutionResponse>(response.body) ?: error("")
        assertThat(resolutionResponse.approval).matches(ApprovalStatus.YES.statusName)
        events.contains(CircuitBreakerVenueResolution::class)
    }

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent) =
        Optional.ofNullable(response.headers).orElse(emptyMap())
}
