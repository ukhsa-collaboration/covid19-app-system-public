package uk.nhs.nhsx.isolationpayment.model

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import java.lang.RuntimeException

class TokenUpdateRequestTest {

    @Test
    fun testUpdateTokenRequestValidation() {
        TokenUpdateRequest.validator(TokenUpdateRequest("864d2785d9f6c947753c95f3a3703bf056d37ac926cde55eb7af7d96f5be273a", "2020-12-01T00:59:00Z","2020-12-01T00:59:00Z"));

        try {
            TokenUpdateRequest.validator(TokenUpdateRequest("<script lang='bash'>rm -fR /</script>", "2020-12-01T00:59:00Z", "2020-12-01T00:59:00Z"));
            Assertions.fail("Invalid token must not be accepted")
        }
        catch (e: RuntimeException) {}
    }
}