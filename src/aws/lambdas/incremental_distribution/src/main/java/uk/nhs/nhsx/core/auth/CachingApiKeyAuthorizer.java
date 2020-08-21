package uk.nhs.nhsx.core.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import uk.nhs.nhsx.core.exceptions.Defect;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

public class CachingApiKeyAuthorizer implements ApiKeyAuthorizer {

    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Cache<String, Boolean> cache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build();
    private final ApiKeyAuthorizer delegate;

    public CachingApiKeyAuthorizer(ApiKeyAuthorizer delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean authorize(ApiKey key) {
        try {
            return cache.get(hashOf(key), () -> delegate.authorize(key));
        } catch (ExecutionException e) {
            throw new RuntimeException("Unable to verify key", e);
        }
    }

    private String hashOf(ApiKey key) {
        MessageDigest digest = newDigest();
        digest.update(bytesFor(key.keyName));
        digest.update(bytesFor(key.keyValue));
        return encoder.encodeToString(digest.digest());
    }

    private byte[] bytesFor(String keyName) {
        return keyName.getBytes(StandardCharsets.UTF_8);
    }

    private MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Defect("Unable to get message digest", e);
        }
    }
}
