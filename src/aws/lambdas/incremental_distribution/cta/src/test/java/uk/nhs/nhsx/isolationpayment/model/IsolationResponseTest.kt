package uk.nhs.nhsx.isolationpayment.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.toJson

class IsolationResponseTest {

    @Test
    fun `json serialisation`() {
        expectThat(IsolationResponse(IpcTokenId.of("1".repeat(64)), "hello"))
            .toJson()
            .isEqualToJson("""{"ipcToken":"1111111111111111111111111111111111111111111111111111111111111111","state":"hello","contractVersion":1}""")
    }
}
