package uk.nhs.nhsx.isolationpayment

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TokenGeneratorTest {

    @Test
    fun testIsolationTokenContainsHexadecimalsOnly() {
        val token = TokenGenerator.getToken()
        Assertions.assertThat(token).matches("[A-Fa-f0-9]+")
    }

    @Test
    fun testIsolationTokenLengthIsSixtyFourCharacters() {
        val token = TokenGenerator.getToken()
        Assertions.assertThat(token).hasSize(64)
    }
}