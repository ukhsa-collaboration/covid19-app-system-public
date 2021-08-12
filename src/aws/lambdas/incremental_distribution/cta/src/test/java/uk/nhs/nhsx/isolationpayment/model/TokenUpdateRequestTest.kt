package uk.nhs.nhsx.isolationpayment.model

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isSuccess
import strikt.assertions.message
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.testhelper.data.asInstant

class TokenUpdateRequestTest {

    @Test
    fun `update token request validates`() {
        expectCatching {
            TokenUpdateRequest(
                IpcTokenId.of("864d2785d9f6c947753c95f3a3703bf056d37ac926cde55eb7af7d96f5be273a"),
                "2020-12-01T00:59:00Z".asInstant(),
                "2020-12-01T00:59:00Z".asInstant()
            )
        }.isSuccess()
    }

    @Test
    fun `update token request fails validation`() {
        expectCatching {
            TokenUpdateRequest(
                IpcTokenId.of("<script lang='bash'>rm -fR /</script>"),
                "2020-12-01T00:59:00Z".asInstant(),
                "2020-12-01T00:59:00Z".asInstant()
            )
        }.isFailure()
            .message
            .isA<String>()
            .contains("Validation failed for: (<script lang='bash'>rm -fR /</script>)")
    }
}
