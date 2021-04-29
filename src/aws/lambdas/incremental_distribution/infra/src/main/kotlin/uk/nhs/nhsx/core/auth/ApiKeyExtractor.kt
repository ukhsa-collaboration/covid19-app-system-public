package uk.nhs.nhsx.core.auth

import java.util.*

object ApiKeyExtractor {

    operator fun invoke(authorizationHeader: String?) = authorizationHeader
        ?.trim()
        ?.takeIf { it.startsWith("Bearer ") }
        ?.let { base64Decode(it.substringAfter(' ')) }
        ?.takeIf { it.contains(':') }?.split(':', limit = 2)
        ?.takeIf { (k, v) -> k.isNotBlank() && v.isNotBlank() }
        ?.let { (key, value) -> ApiKey(key, value) }

    private fun base64Decode(value: String?): String? = try {
        value?.toByteArray()?.let { Base64.getDecoder().decode(it) }?.decodeToString()
    } catch (e: Exception) {
        null
    }
}
