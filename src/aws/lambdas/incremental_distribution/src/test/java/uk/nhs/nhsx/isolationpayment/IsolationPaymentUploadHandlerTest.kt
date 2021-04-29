package uk.nhs.nhsx.isolationpayment

import com.amazonaws.HttpMethod.POST
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.domain.IpcTokenId

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

    private val handler =
        IsolationPaymentUploadHandler(environment, authenticator, service, RecordingEvents(), { true })

    @Test
    fun `consuming token returns isolation payment response with given token`() {
        every { service.consumeIsolationToken(any()) } returns IsolationResponse(
            ipcToken,
            TokenStateExternal.EXT_CONSUMED.value
        )

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody(
                """
                {
                    "ipcToken": "$ipcToken"
                }
                """.trimIndent()
            )

            .withPath("/isolation-payment/ipc-token/consume-token")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)

        val bodyIsolationResponse = Json.readJsonOrNull<IsolationResponse>(response.body) ?: error("")
        assertThat(bodyIsolationResponse.contractVersion).isEqualTo(1)
        assertThat(bodyIsolationResponse.ipcToken).isEqualTo(ipcToken)
        assertThat(bodyIsolationResponse.state).isEqualTo(TokenStateExternal.EXT_CONSUMED.value)

        verifySequence {
            service.consumeIsolationToken(ipcToken)
        }
    }

    @Test
    fun `verifying token returns isolation payment response with given token`() {
        every { service.verifyIsolationToken(any()) } returns IsolationResponse(
            ipcToken,
            TokenStateExternal.EXT_VALID.value
        )

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBody(
                """
                {
                    "ipcToken": "$ipcToken"
                }
                """.trimIndent()
            )
            .withPath("/isolation-payment/ipc-token/verify-token")
            .withBearerToken("anything")
            .build()

        val response = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(response.statusCode).isEqualTo(200)

        val bodyIsolationResponse = Json.readJsonOrNull<IsolationResponse>(response.body) ?: error("")
        assertThat(bodyIsolationResponse.contractVersion).isEqualTo(1)
        assertThat(bodyIsolationResponse.ipcToken).isEqualTo(ipcToken)
        assertThat(bodyIsolationResponse.state).isEqualTo(TokenStateExternal.EXT_VALID.value)

        verifySequence {
            service.verifyIsolationToken(ipcToken)
        }
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
    }
}
