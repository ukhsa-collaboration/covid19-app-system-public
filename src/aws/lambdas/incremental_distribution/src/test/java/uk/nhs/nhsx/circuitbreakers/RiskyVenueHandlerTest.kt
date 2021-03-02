package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson.readMaybe
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
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import java.util.Optional

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
        ), { true }, signer, breaker, events, { true }
    )

    @Test
    fun `handle circuit breaker request with venue id`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")
            .withJson("{\"venueId\": \"MAX8CHR1\"}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val tokenResponse = readMaybe(
            response.body,
            TokenResponse::class.java
        ) { }.orElse(TokenResponse())
        assertThat(tokenResponse.approval).matches("pending")
        assertThat(tokenResponse.approvalToken).matches("[a-zA-Z0-9]+")
        events.containsExactly(CircuitBreakerVenueRequest::class)
    }

    @Test
    fun `handle circuit breaker request invalid json data`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")
            .withJson("{\"invalidField\": null}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle circuit breaker request no body`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `handle circuit breaker no such path`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/unknown-feature")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(404)
    }

    @Test
    fun `handle circuit breaker missing token`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/resolution")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
    }

    @Test
    fun `handle circuit breaker resolution success`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/resolution/abc123")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val resolutionResponse = readMaybe(response.body, ResolutionResponse::class.java) {}
            .orElse(ResolutionResponse())
        assertThat(resolutionResponse.approval).matches(ApprovalStatus.YES.getName())
        events.containsExactly(CircuitBreakerVenueResolution::class)
    }

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent): Map<String, String> {
        return Optional.ofNullable(response.headers).orElse(emptyMap())
    }
}
