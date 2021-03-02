package uk.nhs.nhsx.isolationpayment

import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson.readMaybe
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import java.util.Optional

class IsolationPaymentOrderHandlerTest {

    private val environment = TestEnvironments.TEST.apply(
        mapOf(
            "MAINTENANCE_MODE" to "false",
            "TOKEN_CREATION_ENABLED" to "true",
            "custom_oai" to "OAI"
        )
    )

    private val environmentTokenCreationDisabled = TestEnvironments.TEST.apply(
        mapOf(
            "MAINTENANCE_MODE" to "false",
            "TOKEN_CREATION_ENABLED" to "false",
            "custom_oai" to "OAI"
        )
    )

    private val authenticator = Authenticator { true }

    private val contentSigner = mockk<Signer> {
        every { sign(any()) } returns
            Signature(
                KeyId.of("TEST_KEY_ID"),
                SigningAlgorithmSpec.ECDSA_SHA_256, "TEST_SIGNATURE".toByteArray()
            )
    }

    private val events = RecordingEvents()
    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner), events)

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent): Map<String, String> =
        Optional.ofNullable(response.headers).orElse(emptyMap())

    private val service = mockk<IsolationPaymentMobileService>()

    private val handler = IsolationPaymentOrderHandler(
        environment,
        authenticator,
        signer,
        service,
        RecordingEvents(),
        { true }
    )

    @Test
    fun `token create returns 201 when tokens is created`() {
        every { service.handleIsolationPaymentOrder(any()) } returns TokenGenerationResponse(true, "some-id")

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody(
                """
                {
                    "country": "England"
                }
                """.trimIndent()
            )
            .withPath("/isolation-payment/ipc-token/create")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(201)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val tokenGenerationResponse =
            readMaybe(
                response.body,
                TokenGenerationResponse::class.java
            ) { }
                .orElseThrow()

        assertThat(tokenGenerationResponse.ipcToken).isEqualTo("some-id")
        assertThat(tokenGenerationResponse.isEnabled).isEqualTo(true)
    }

    @Test
    fun `token create returns 400 when invalid request`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("{}")
            .withPath("/isolation-payment/ipc-token/create")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(400)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `token create returns 503 when feature is disabled`() {
        val handler = IsolationPaymentOrderHandler(
            environmentTokenCreationDisabled,
            authenticator,
            signer,
            service,
            RecordingEvents(),
            { true }
        )

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody(
                """
                {
                    "country": "England"
                }
                """.trimIndent()
            )
            .withPath("/isolation-payment/ipc-token/create")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(503)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `token update returns 200 when tokens is updated`() {
        every { service.handleIsolationPaymentUpdate(any()) } returns TokenUpdateResponse("https://test?ipcToken=some-id")

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/isolation-payment/ipc-token/update")
            .withBearerToken("anything")
            .withBody(
                """
                {
                    "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
                    "riskyEncounterDate": "2020-08-24T21:59:00Z",
                    "isolationPeriodEndDate": "2020-08-24T21:59:00Z"
                }
            """.trimIndent()
            )
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")

        val tokenUpdateResponse = readMaybe(response.body, TokenUpdateResponse::class.java) { }.orElseThrow()

        assertThat(tokenUpdateResponse.websiteUrlWithQuery).isEqualTo("https://test?ipcToken=some-id")
    }

    @Test
    fun `token update returns 400 when invalid request`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("{}")
            .withPath("/isolation-payment/ipc-token/update")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(400)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `token update returns 503 when feature is disabled`() {
        val handler = IsolationPaymentOrderHandler(
            environmentTokenCreationDisabled,
            authenticator,
            signer,
            service,
            RecordingEvents(),
            { true }
        )

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody(
                """
                {
                    "ipcToken": "7fde14a2ee03611b0b511c8f88b3e6d49b658ad5002c3a5fb81d25ab54d4b8ac",
                    "riskyEncounterDate": "2020-08-24T21:59:00Z",
                    "isolationPeriodEndDate": "2020-08-24T21:59:00Z"
                }
            """.trimIndent()
            )
            .withPath("/isolation-payment/ipc-token/update")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(503)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `returns 404 for unknown paths`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/unknown/path")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.body).isNull()
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }

    @Test
    fun `returns 200 if health is ok`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/isolation-payment/health")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature")
    }
}
