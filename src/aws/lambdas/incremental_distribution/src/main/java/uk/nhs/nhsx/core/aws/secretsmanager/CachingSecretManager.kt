package uk.nhs.nhsx.core.aws.secretsmanager

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.util.*

class CachingSecretManager(delegate: SecretManager) : SecretManager {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build(delegate::getSecret)

    private val cacheBinary = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build(delegate::getSecretBinary)

    override fun getSecret(secretName: SecretName): Optional<SecretValue> = try {
        cache[secretName]!!
    } catch (e: Exception) {
        throw RuntimeException("Unable to load secret", e)
    }

    override fun getSecretBinary(secretName: SecretName): ByteArray = try {
        cacheBinary[secretName]!!
    } catch (e: Exception) {
        throw RuntimeException("Unable to load secret", e)
    }
}
