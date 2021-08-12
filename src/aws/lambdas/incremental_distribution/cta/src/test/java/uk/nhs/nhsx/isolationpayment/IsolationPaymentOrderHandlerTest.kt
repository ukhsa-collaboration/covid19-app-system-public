package uk.nhs.nhsx.isolationpayment

import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec.ECDSA_SHA_256
import io.mockk.every
import io.mockk.mockk
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.OK
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.headers
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.assertions.withReadJsonOrThrow
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class IsolationPaymentOrderHandlerTest {

    private val environment = TEST.apply(
        mapOf(
            "MAINTENANCE_MODE" to "false",
            "TOKEN_CREATION_ENABLED" to "true",
            "custom_oai" to "OAI"
        )
    )

    private val environmentTokenCreationDisabled = TEST.apply(
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
                ECDSA_SHA_256, "TEST_SIGNATURE".toByteArray()
            )
    }

    private val events = RecordingEvents()
    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner), events)
    private val service = mockk<IsolationPaymentMobileService>()

    private val handler = IsolationPaymentOrderHandler(
        environment = environment,
        clock = SystemClock.CLOCK,
        events = RecordingEvents(),
        authenticator = authenticator,
        signer = signer,
        service = service
    ) { true }

    @Test
    fun `token create returns 201 when tokens is created`() {
        every { service.handleIsolationPaymentOrder(any()) } returns OK(IpcTokenId.of("1".repeat(64)))

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("""{ "country": "England" }""")
            .withPath("/isolation-payment/ipc-token/create")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(CREATED)
            headers.containsKey("x-amz-meta-signature")
            body.withReadJsonOrThrow<OK> {
                get(TokenGenerationResponse.OK::ipcToken).isEqualTo(IpcTokenId.of("1".repeat(64)))
                get(TokenGenerationResponse.OK::isEnabled).isTrue()
            }
        }
    }

    @Test
    fun `token create returns 400 when invalid request`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("{}")
            .withPath("/isolation-payment/ipc-token/create")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(BAD_REQUEST)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `token create returns 503 when feature is disabled`() {
        val handler = IsolationPaymentOrderHandler(
            environmentTokenCreationDisabled,
            SystemClock.CLOCK,
            RecordingEvents(),
            authenticator,
            signer,
            service
        ) { true }

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("""{ "country": "England" }""")
            .withPath("/isolation-payment/ipc-token/create")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(SERVICE_UNAVAILABLE)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `token update returns 200 when tokens is updated`() {
        every { service.handleIsolationPaymentUpdate(any()) } returns TokenUpdateResponse("https://test?ipcToken=some-id")

        val requestEvent = request()
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
                """
            )

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.withReadJsonOrThrow<TokenUpdateResponse> {
                get(TokenUpdateResponse::websiteUrlWithQuery).isEqualTo("https://test?ipcToken=some-id")
            }
        }
    }

    @Test
    fun `token update returns 400 when invalid request`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("{}")
            .withPath("/isolation-payment/ipc-token/update")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(BAD_REQUEST)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `token update returns 503 when feature is disabled`() {
        val handler = IsolationPaymentOrderHandler(
            environmentTokenCreationDisabled,
            SystemClock.CLOCK,
            RecordingEvents(),
            authenticator,
            signer,
            service
        ) { true }

        val requestEvent = request()
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
                """
            )
            .withPath("/isolation-payment/ipc-token/update")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(SERVICE_UNAVAILABLE)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `returns 404 for unknown paths`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/unknown/path")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(NOT_FOUND)
            headers.containsKey("x-amz-meta-signature")
            body.isNull()
        }
    }

    @Test
    fun `returns 200 if health is ok`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/isolation-payment/health")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
        }
    }
}
