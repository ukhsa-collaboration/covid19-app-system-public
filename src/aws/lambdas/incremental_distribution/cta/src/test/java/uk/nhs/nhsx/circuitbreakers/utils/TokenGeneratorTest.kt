package uk.nhs.nhsx.circuitbreakers.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TokenGeneratorTest {

    @Test
    fun testTokenContainsOnlyAlphanumericCharacters() {
        val token = TokenGenerator.token
        assertThat(token).matches("[A-Za-z0-9]+")
    }

    @Test
    fun testTokenLengthIsFiftyCharacters() {
        val token = TokenGenerator.token
        assertThat(token).hasSize(50)
    }
}
