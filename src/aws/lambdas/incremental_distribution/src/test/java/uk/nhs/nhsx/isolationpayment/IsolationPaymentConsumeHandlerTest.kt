package uk.nhs.nhsx.isolationpayment

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.isolationpayment.model.IsolationRequest
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse

class IsolationPaymentConsumeHandlerTest {

    private val ipcToken = "ipc-token"
    private val state = "state"

    private val service = mockk<IsolationPaymentGatewayService>()
    private val handler = IsolationPaymentConsumeHandler(service)

    @Test
    fun `returns isolation payment response with given token`() {
        every { service.consumeIsolationToken(any()) } returns IsolationResponse(ipcToken, state)

        val response = handler.handleRequest(IsolationRequest(ipcToken), ContextBuilder.aContext())

        assertThat(response.contractVersion).isEqualTo(1)
        assertThat(response.ipcToken).isEqualTo(ipcToken)
        assertThat(response.state).isEqualTo(state)

        verifySequence {
            service.consumeIsolationToken(ipcToken)
        }
    }

    @Test
    fun `throws when isolation request is invalid`() {
        assertThatThrownBy { handler.handleRequest(null, ContextBuilder.aContext()) }
            .isInstanceOf(RuntimeException::class.java)
    }
}