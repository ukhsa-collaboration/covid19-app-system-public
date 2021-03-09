package uk.nhs.nhsx.isolationpayment.model

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.virology.IpcTokenId

class IsolationResponseTest {

    @Test
    fun `json serialisation`() {
        assertEquals("""{"ipcToken":"1111111111111111111111111111111111111111111111111111111111111111","state":"hello","contractVersion":1}""",
            Jackson.toJson(IsolationResponse(IpcTokenId.of("1".repeat(64)), "hello")), JSONCompareMode.STRICT)
    }
}
