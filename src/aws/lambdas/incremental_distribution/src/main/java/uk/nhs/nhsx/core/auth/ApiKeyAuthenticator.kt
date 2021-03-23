package uk.nhs.nhsx.core.auth

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events

data class ApiKeyAuthenticationFailed(private val authorizationHeader: String) : Event(EventCategory.Info)

class ApiKeyAuthenticator(private val events: Events, private val authorizer: ApiKeyAuthorizer) : Authenticator {
    override fun isAuthenticated(authorizationHeader: String): Boolean {
        val success = ApiKeyExtractor(authorizationHeader)
            ?.let { authorizer.authorize(it) }
            ?: false

        if (!success) {
            events(ApiKeyAuthenticationFailed(authorizationHeader))
        }

        return success
    }
    companion object {
        fun authenticatingWithApiKey(events: Events, authorizer: ApiKeyAuthorizer): ApiKeyAuthenticator =
            ApiKeyAuthenticator(events, authorizer)
    }
}
