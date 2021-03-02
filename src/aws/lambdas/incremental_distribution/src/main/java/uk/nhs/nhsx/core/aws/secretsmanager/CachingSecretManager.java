package uk.nhs.nhsx.core.aws.secretsmanager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.Optional;

public class CachingSecretManager implements SecretManager {

    private final LoadingCache<SecretName, Optional<SecretValue>> cache;
    private final LoadingCache<SecretName, byte[]> cacheBinary;

    public CachingSecretManager(SecretManager delegate) {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(delegate::getSecret);

        this.cacheBinary = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(delegate::getSecretBinary);
    }

    @Override
    public Optional<SecretValue> getSecret(SecretName secretName) {
        try {
            return cache.get(secretName);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load secret", e);
        }
    }

    @Override
    public byte[] getSecretBinary(SecretName secretName) {
        try {
            return cacheBinary.get(secretName);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load secret", e);
        }
    }
}
