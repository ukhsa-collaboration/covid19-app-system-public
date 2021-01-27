package uk.nhs.nhsx.core.auth

import com.amazonaws.util.Base64
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.auth.ApiKeyAuthenticator.authenticatingWithApiKey
import java.nio.charset.StandardCharsets

class ApiKeyAuthenticatorTest {

    private val authenticator = authenticatingWithApiKey({ true })

    @Test
    fun usesAllAuthorizers() {
        assertThat(authenticatingWithApiKey({ true }, { true }).isAuthenticated(valid), `is`(true))
        assertThat(authenticatingWithApiKey({ true }, { false }).isAuthenticated(valid), `is`(false))
        assertThat(authenticatingWithApiKey({ false }, { true }).isAuthenticated(valid), `is`(false))
        assertThat(authenticatingWithApiKey({ false }, { false }).isAuthenticated(valid), `is`(false))
    }

    @Test
    fun lazy() {
        assertThat(authenticatingWithApiKey({ false }, { throw IllegalArgumentException() })
            .isAuthenticated(valid), `is`(false))
    }

    @Test
    fun handlesEmptyAuthHeader() {
        val authorizationHeader = ""
        assertThat(authenticator.isAuthenticated(authorizationHeader), `is`(false))
    }

    @Test
    fun handlesEmptyApiKey() {
        val apiKey = ""
        val authorizationHeader = "Bearer $apiKey"
        assertThat(authenticator.isAuthenticated(authorizationHeader), `is`(false))
    }

    @Test
    fun handlesApiKeyWithEmptyKeyNameAndKeyValue() {
        val apiKey = apiKeyFrom("", "")
        val authorizationHeader = "Bearer $apiKey"
        assertThat(authenticator.isAuthenticated(authorizationHeader), `is`(false))
    }

    @Test
    fun handlesApiKeyWithEmptyKeyName() {
        val apiKey = apiKeyFrom("", "value")
        val authorizationHeader = "Bearer $apiKey"
        assertThat(authenticator.isAuthenticated(authorizationHeader), `is`(false))
    }

    @Test
    fun handlesApiKeyWithEmptyKeyValue() {
        val apiKey = apiKeyFrom("name", "")
        val authorizationHeader = "Bearer $apiKey"
        assertThat(authenticator.isAuthenticated(authorizationHeader), `is`(false))
    }

    @Test
    fun handlesApiKeyWithNonBase64Encoding() {
        val apiKey = "name:value"
        val authorizationHeader = "Bearer $apiKey"
        assertThat(authenticator.isAuthenticated(authorizationHeader), `is`(false))
    }

    @Test
    fun doesRightThingWhenKeyContainsAColon() {
        val apiKey = "name:value:blah"
        val authorizationHeader = "Bearer $apiKey"
        val authenticator = authenticatingWithApiKey(ApiKeyAuthorizer { k: ApiKey ->
            assertThat(k.keyName, equalTo(apiKey))
            true
        })
        assertThat(authenticator.isAuthenticated(authorizationHeader), `is`(false))
    }

    private fun apiKeyFrom(keyName: String, keyValue: String): String {
        val keyNameValue = "$keyName:$keyValue"
        return java.util.Base64.getEncoder().encodeToString(keyNameValue.toByteArray())
    }

    companion object {
        private val valid = "Bearer " + Base64.encodeAsString(*"k:v".toByteArray(StandardCharsets.UTF_8))
    }
}