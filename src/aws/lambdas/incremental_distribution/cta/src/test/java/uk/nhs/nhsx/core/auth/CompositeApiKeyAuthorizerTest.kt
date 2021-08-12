package uk.nhs.nhsx.core.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.message

class CompositeApiKeyAuthorizerTest {

    private val apiKey = ApiKey("name", "value")

    @ParameterizedTest
    @CsvSource(
        value = [
            "true,true,true",
            "true,false,false",
            "false,true,false",
            "false,false,false",
        ]
    )
    fun `returns true only when all authorizers are happy`(first: Boolean, second: Boolean, expected: Boolean) {
        fun assertAuthFor(first: Boolean, second: Boolean, expected: Boolean) {
            expectThat(CompositeApiKeyAuthorizer(
                ApiKeyAuthorizer { first },
                ApiKeyAuthorizer { second }
            ).authorize(apiKey)).isEqualTo(expected)
        }

        assertAuthFor(first, second, expected)
    }

    @Test
    fun `stops evaluating authorizers after the first failure`() {
        val authorizer = CompositeApiKeyAuthorizer(
            ApiKeyAuthorizer { false },
            ApiKeyAuthorizer { throw IllegalStateException("I am broken") }
        )

        expectThat(authorizer.authorize(apiKey)).isFalse()
    }

    @Test
    fun `throws exception for empty list of authorizers`() {
        expectThrows<IllegalStateException> { CompositeApiKeyAuthorizer() }
            .message.isEqualTo("must contain at least one ApiKeyAuthorizer")
    }
}
