package uk.nhs.nhsx.core.auth

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CompositeApiKeyAuthorizerTest {

    private val apiKey = ApiKey("name", "value")

    @Test
    fun `returns true only when all authorizers are happy`() {
        fun assertAuthFor(first: Boolean, second: Boolean, expected: Boolean) {
            assertThat(
                CompositeApiKeyAuthorizer(ApiKeyAuthorizer { first }, ApiKeyAuthorizer { second }).authorize(apiKey)
            ).isEqualTo(expected)
        }

        assertAuthFor(first = true, second = true, expected = true)
        assertAuthFor(first = true, second = false, expected = false)
        assertAuthFor(first = false, second = true, expected = false)
        assertAuthFor(first = false, second = false, expected = false)
    }

    @Test
    fun `stops evaluating authorizers after the first failure`() {
        val authorizer = CompositeApiKeyAuthorizer(
            ApiKeyAuthorizer { false },
            ApiKeyAuthorizer { throw IllegalStateException("I am broken") }
        )

        assertThat(authorizer.authorize(apiKey)).isEqualTo(false)
    }

    @Test
    fun `throws exception for empty list of authorizers`() {
        assertThatThrownBy { CompositeApiKeyAuthorizer() }
            .hasMessage("must contain at least one ApiKeyAuthorizer")
    }
}
