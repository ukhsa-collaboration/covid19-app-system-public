package uk.nhs.nhsx.core.auth

fun interface Authenticator {
    fun isAuthenticated(authorizationHeader: String): Boolean
}
