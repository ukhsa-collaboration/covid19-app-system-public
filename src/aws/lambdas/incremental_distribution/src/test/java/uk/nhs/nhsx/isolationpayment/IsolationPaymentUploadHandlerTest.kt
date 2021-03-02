package uk.nhs.nhsx.isolationpayment

import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import java.util.*

class IsolationPaymentUploadHandlerTest {
    private val ipcToken = "ipc-token"

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

    private fun headersOrEmpty(response: APIGatewayProxyResponseEvent): Map<String, String> =
        Optional.ofNullable(response.headers).orElse(emptyMap())

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

        val bodyIsolationResponse = Jackson.readMaybe(
            response.body,
            IsolationResponse::class.java
        ) { }
        assertThat(bodyIsolationResponse.isPresent).isEqualTo(true)
        assertThat(bodyIsolationResponse.get().contractVersion).isEqualTo(1)
        assertThat(bodyIsolationResponse.get().ipcToken).isEqualTo(ipcToken)
        assertThat(bodyIsolationResponse.get().state).isEqualTo(TokenStateExternal.EXT_CONSUMED.value)

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

        val bodyIsolationResponse = Jackson.readMaybe(
            response.body,
            IsolationResponse::class.java
        ) { }
        assertThat(bodyIsolationResponse.isPresent).isEqualTo(true)
        assertThat(bodyIsolationResponse.get().contractVersion).isEqualTo(1)
        assertThat(bodyIsolationResponse.get().ipcToken).isEqualTo(ipcToken)
        assertThat(bodyIsolationResponse.get().state).isEqualTo(TokenStateExternal.EXT_VALID.value)

        verifySequence {
            service.verifyIsolationToken(ipcToken)
        }
    }

    @Test
    fun `throws when isolation request is invalid`() {

        assertThatThrownBy { handler.handleRequest(null, ContextBuilder.aContext()) }
            .isInstanceOf(RuntimeException::class.java)
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
