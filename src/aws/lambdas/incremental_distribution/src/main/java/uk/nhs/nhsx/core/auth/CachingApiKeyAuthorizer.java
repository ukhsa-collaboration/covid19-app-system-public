package uk.nhs.nhsx.core.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import uk.nhs.nhsx.core.exceptions.Defect;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CachingApiKeyAuthorizer implements ApiKeyAuthorizer {

    private final Base64.Encoder encoder = Base64.getEncoder();

    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build();

    private final ApiKeyAuthorizer delegate;

    public CachingApiKeyAuthorizer(ApiKeyAuthorizer delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean authorize(ApiKey key) {
        try {
            return authorizeInternal(key);
        } catch (Exception e) {
            throw new RuntimeException("Unable to verify key", e);
        }
    }

    private Boolean authorizeInternal(ApiKey key) {
        return Optional.ofNullable(cache.get(hashOf(key), k -> delegate.authorize(key))).orElse(false);
    }

    private String hashOf(ApiKey key) {
        MessageDigest digest = newDigest();
        digest.update(bytesFor(key.getKeyName()));
        digest.update(bytesFor(key.getKeyValue()));
        return encoder.encodeToString(digest.digest());
    }

    private MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Defect("Unable to get message digest", e);
        }
    }

    private byte[] bytesFor(String keyName) {
        return keyName.getBytes(UTF_8);
    }
}
