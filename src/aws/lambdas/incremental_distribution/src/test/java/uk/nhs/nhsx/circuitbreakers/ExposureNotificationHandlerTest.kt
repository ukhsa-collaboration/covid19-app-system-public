package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.HttpMethod
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import uk.nhs.nhsx.ContextBuilder
import uk.nhs.nhsx.ProxyRequestBuilder
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import java.util.*

class ExposureNotificationHandlerTest {

    private val contentSigner = mock(Signer::class.java)
    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner))
    private val handler = ExposureNotificationHandler(Authenticator { true }, signer)

    @Before
    fun setUpMock() {
        `when`(contentSigner.sign(ArgumentMatchers.any()))
            .thenReturn(
                Signature(
                    KeyId.of("some-id"),
                    SigningAlgorithmSpec.ECDSA_SHA_256,
                    "TEST_SIGNATURE".toByteArray()
                )
            )
    }

    @Test
    fun handleCircuitBreakerRequestSuccess() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson("""{
                "matchedKeyCount": 2,
                "daysSinceLastExposure": 3,
                "maximumRiskScore": 150
            }"""
            ).build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val tokenResponse = Jackson.deserializeMaybe(response.body, TokenResponse::class.java)
            .orElse(TokenResponse())
        assertThat(tokenResponse.approval).matches("yes")
        assertThat(tokenResponse.approvalToken).matches("[a-zA-Z0-9]+")
    }

    @Test
    fun handleCircuitBreakerRequestInvalidJsonData() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson("{\"invalidField\": null}")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun handleCircuitBreakerRequestInvalidJsonFormat() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson("{ invalid }")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun handleCircuitBreakerRequestNoBody() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(422)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun handleCircuitBreakerNoSuchPath() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/circuit-breaker/unknown-feature")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(404)
    }

    @Test
    fun handleCircuitBreakerMissingToken() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath("/circuit-breaker/exposure-notification/resolution")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(404)
    }

    @Test
    fun handleCircuitBreakerResolutionSuccess() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath("/circuit-breaker/exposure-notification/resolution/abc123")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val resolutionResponse = Jackson.deserializeMaybe(response.body, ResolutionResponse::class.java)
            .orElse(ResolutionResponse())
        assertThat(resolutionResponse.approval).matches(ApprovalStatus.YES.getName())
    }

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent): Map<String, String> {
        return Optional.ofNullable(response.headers).orElse(emptyMap())
    }
}