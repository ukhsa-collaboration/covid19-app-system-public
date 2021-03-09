package uk.nhs.nhsx.isolationpayment.model

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.virology.IpcTokenId

class TokenUpdateRequestTest {

    @Test
    fun `update token request validates`() {
        TokenUpdateRequest(
            IpcTokenId.of("864d2785d9f6c947753c95f3a3703bf056d37ac926cde55eb7af7d96f5be273a"),
            "2020-12-01T00:59:00Z".asInstant(),
            "2020-12-01T00:59:00Z".asInstant()
        )
    }

    @Test
    fun `update token request fails validation`() {
        assertThatThrownBy {
            TokenUpdateRequest(
                IpcTokenId.of("<script lang='bash'>rm -fR /</script>"),
                "2020-12-01T00:59:00Z".asInstant(),
                "2020-12-01T00:59:00Z".asInstant()
            )
        }.hasMessage("Validation failed for: (<script lang='bash'>rm -fR /</script>)")
    }
}
