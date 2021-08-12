package uk.nhs.nhsx.isolationpayment

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationRequest
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext

class IsolationPaymentConsumeHandlerTest {

    private val ipcToken = IpcTokenId.of("1".repeat(64))
    private val state = "state"

    private val service = mockk<IsolationPaymentGatewayService>()
    private val handler = IsolationPaymentConsumeHandler(service, RecordingEvents())

    @Test
    fun `returns isolation payment response with given token`() {
        every { service.consumeIsolationToken(any()) } returns IsolationResponse(ipcToken, state)

        val response = handler.handler()(IsolationRequest(ipcToken), aContext())

        expectThat(response) {
            get(IsolationResponse::contractVersion).isEqualTo(1)
            get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
            get(IsolationResponse::state).isEqualTo(state)
        }

        verifySequence {
            service.consumeIsolationToken(ipcToken)
        }
    }
}
