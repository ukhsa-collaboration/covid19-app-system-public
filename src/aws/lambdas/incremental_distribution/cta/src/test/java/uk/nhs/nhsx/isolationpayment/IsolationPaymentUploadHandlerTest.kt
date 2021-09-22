package uk.nhs.nhsx.isolationpayment

import com.amazonaws.HttpMethod.POST
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_CONSUMED
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_VALID
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.assertions.withReadJsonOrThrow
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class IsolationPaymentUploadHandlerTest {

    private val ipcToken = IpcTokenId.of("1".repeat(64))
    private val service = mockk<IsolationPaymentGatewayService>()
    private val authenticator = Authenticator { true }
    private val environment = TestEnvironments.TEST.apply(
        mapOf(
            "MAINTENANCE_MODE" to "false",
            "TOKEN_CREATION_ENABLED" to "true",
            "custom_oai" to "OAI"
        )
    )

    private val handler = IsolationPaymentUploadHandler(
        environment = environment,
        authenticator = authenticator,
        service = service,
        events = RecordingEvents()
    ) { true }

    @Test
    fun `consuming token returns isolation payment response with given token`() {
        every { service.consumeIsolationToken(any()) } returns IsolationResponse.of(ipcToken, EXT_CONSUMED)

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("""{ "ipcToken": "$ipcToken" }""")
            .withPath("/isolation-payment/ipc-token/consume-token")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            body.withReadJsonOrThrow<IsolationResponse> {
                get(IsolationResponse::contractVersion).isEqualTo(1)
                get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
                get(IsolationResponse::state).isEqualTo(EXT_CONSUMED.value)
            }
        }

        verifySequence {
            service.consumeIsolationToken(ipcToken)
        }
    }

    @Test
    fun `verifying token returns isolation payment response with given token`() {
        every { service.verifyIsolationToken(any()) } returns IsolationResponse.of(ipcToken, EXT_VALID)

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody("""{ "ipcToken": "$ipcToken" } """)
            .withPath("/isolation-payment/ipc-token/verify-token")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            body.withReadJsonOrThrow<IsolationResponse> {
                get(IsolationResponse::contractVersion).isEqualTo(1)
                get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
                get(IsolationResponse::state).isEqualTo(EXT_VALID.value)
            }
        }

        verifySequence {
            service.verifyIsolationToken(ipcToken)
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
        }
    }
}
