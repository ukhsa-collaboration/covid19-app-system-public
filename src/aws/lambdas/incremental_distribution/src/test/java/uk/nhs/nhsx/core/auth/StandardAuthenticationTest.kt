package uk.nhs.nhsx.core.auth

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class StandardAuthenticationTest {

    @Test
    fun anApi() {
        assertSomeKeyNames(api())
    }

    @Test
    fun mobileApi() {
        assertSomeKeyNames(api())
    }

    private fun assertSomeKeyNames(authorizer: ApiKeyAuthorizer) {
        assertThat(authorizer.authorize(ApiKey("mobile", "")), `is`(true))
        assertThat(authorizer.authorize(ApiKey("blah_c-_134", "")), `is`(true))
        assertThat(authorizer.authorize(ApiKey("third-party-integration-2020102", "")), `is`(true))
        assertThat(authorizer.authorize(ApiKey("-/_+=.@!", "")), `is`(false))
        assertThat(authorizer.authorize(ApiKey("©∞", "")), `is`(false))
        assertThat(authorizer.authorize(ApiKey("0123456789012345678900123456789012345678901234567890", "")), `is`(false))
    }

    private fun api(): ApiKeyAuthorizer = StandardAuthentication.apiKeyNameValidator()
}
