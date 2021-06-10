package uk.nhs.nhsx.core.auth

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.auth.ApiKeyAuthenticator.Companion.authenticatingWithApiKey
import uk.nhs.nhsx.core.events.RecordingEvents

class ApiKeyAuthenticatorTest {

    private val events = RecordingEvents()

    @Test
    fun `emits events on failure`() {
        val authenticator = authenticatingWithApiKey(events) { false }
        assertThat(authenticator.isAuthenticated(""), equalTo(false))
        events.containsExactly(ApiKeyAuthenticationFailed::class)
    }

    @Test
    fun authenticates() {
        val authenticator = authenticatingWithApiKey(events) { true }
        assertThat(authenticator.isAuthenticated("Bearer ${"key:value".base64Encode()}"), equalTo(true))
    }
}
