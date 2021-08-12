package uk.nhs.nhsx.core.auth

import org.http4k.base64Encode
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.auth.ApiKeyAuthenticator.Companion.authenticatingWithApiKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.assertions.containsExactly

class ApiKeyAuthenticatorTest {

    private val events = RecordingEvents()

    @Test
    fun `emits events on failure`() {
        val authenticator = authenticatingWithApiKey(events) { false }

        expectThat(authenticator.isAuthenticated("")).isFalse()
        expectThat(events).containsExactly(ApiKeyAuthenticationFailed::class)
    }

    @Test
    fun authenticates() {
        val authenticator = authenticatingWithApiKey(events) { true }

        expectThat(authenticator.isAuthenticated("Bearer ${"key:value".base64Encode()}")).isTrue()
    }
}
