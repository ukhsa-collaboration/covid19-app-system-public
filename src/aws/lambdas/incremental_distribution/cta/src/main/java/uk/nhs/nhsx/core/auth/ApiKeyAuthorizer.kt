package uk.nhs.nhsx.core.auth

fun interface ApiKeyAuthorizer {
    fun authorize(key: ApiKey): Boolean
}
