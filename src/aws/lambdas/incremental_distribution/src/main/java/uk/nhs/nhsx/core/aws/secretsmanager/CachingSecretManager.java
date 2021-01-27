package uk.nhs.nhsx.core.aws.secretsmanager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class CachingSecretManager implements SecretManager {

    private final LoadingCache<SecretName, Optional<SecretValue>> cache;
    private final LoadingCache<SecretName, byte[]> cacheBinary;

    public CachingSecretManager(SecretManager delegate) {
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .build(
                    new CacheLoader<>() {
                        @Override
                        public Optional<SecretValue> load(SecretName secretName) {
                            return delegate.getSecret(secretName);
                        }
                    }
                );

        cacheBinary = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(
                new CacheLoader<>() {
                    @Override
                    public byte[] load(SecretName secretName) {
                        return delegate.getSecretBinary(secretName);
                    }
                }
            );
    }

    @Override
    public Optional<SecretValue> getSecret(SecretName secretName) {
        try {
            return cache.get(secretName);
        } catch (ExecutionException e) {
            throw new RuntimeException("Unable to load secret", e);
        }
    }

    @Override
    public byte[] getSecretBinary(SecretName secretName) {
        try {
            return cacheBinary.get(secretName);
        } catch (ExecutionException e) {
            throw new RuntimeException("Unable to load secret", e);
        }
    }
}
