package uk.nhs.nhsx.core.auth

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class StandardAuthenticationTest {

    @Test
    fun `an api`() {
        assertSomeKeyNames(api())
    }

    @Test
    fun `mobile api`() {
        assertSomeKeyNames(api())
    }

    private fun assertSomeKeyNames(authorizer: ApiKeyAuthorizer) {
        expectThat(authorizer.authorize(ApiKey("mobile", ""))).isTrue()
        expectThat(authorizer.authorize(ApiKey("blah_c-_134", ""))).isTrue()
        expectThat(authorizer.authorize(ApiKey("third-party-integration-2020102", ""))).isTrue()
        expectThat(authorizer.authorize(ApiKey("-/_+=.@!", ""))).isFalse()
        expectThat(authorizer.authorize(ApiKey("©∞", ""))).isFalse()
        expectThat(authorizer.authorize(ApiKey("0123456789012345678900123456789012345678901234567890", ""))).isFalse()
    }

    private fun api() = StandardAuthentication.apiKeyNameValidator()
}
