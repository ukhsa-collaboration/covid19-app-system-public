package uk.nhs.nhsx.circuitbreakers.utils

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.length
import strikt.assertions.matches

class TokenGeneratorTest {

    @Test
    fun `test token contains only alphanumeric characters`() {
        val token = TokenGenerator.token
        expectThat(token).matches(Regex("[A-Za-z0-9]+"))
    }

    @Test
    fun `test token length is fifty characters`() {
        val token = TokenGenerator.token
        expectThat(token).length.isEqualTo(50)
    }
}
