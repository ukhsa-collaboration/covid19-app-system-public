package uk.nhs.nhsx.core.auth

import com.github.benmanes.caffeine.cache.Caffeine
import uk.nhs.nhsx.core.exceptions.Defect
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.util.Base64
import java.util.Optional

class CachingApiKeyAuthorizer(private val delegate: ApiKeyAuthorizer) : ApiKeyAuthorizer {
    private val encoder = Base64.getEncoder()
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build<String, Boolean>()

    override fun authorize(key: ApiKey) = try {
        authorizeInternal(key)
    } catch (e: Exception) {
        throw RuntimeException("Unable to verify key", e)
    }

    private fun authorizeInternal(key: ApiKey): Boolean =
        Optional.ofNullable(cache[hashOf(key), { delegate.authorize(key) }]).orElse(false)

    private fun hashOf(key: ApiKey) = encoder.encodeToString(newDigest().apply {
        update(bytesFor(key.keyName))
        update(bytesFor(key.keyValue))
    }.digest())

    private fun newDigest() = try {
        MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
        throw Defect("Unable to get message digest", e)
    }

    private fun bytesFor(keyName: String) = keyName.toByteArray()
}
