package uk.nhs.nhsx.core.auth

class CompositeApiKeyAuthorizer(private vararg val authorizers: ApiKeyAuthorizer) : ApiKeyAuthorizer {
    init {
        if (authorizers.isEmpty()) {
            throw IllegalStateException("must contain at least one ApiKeyAuthorizer")
        }
    }

    override fun authorize(key: ApiKey): Boolean = authorizers.firstOrNull { !it.authorize(key) } == null
}
