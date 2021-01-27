package uk.nhs.nhsx.core.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ApiKeyAuthenticator implements Authenticator {

    private static final Logger logger = LogManager.getLogger(ApiKeyAuthenticator.class);
    private final List<ApiKeyAuthorizer> authorizers;

    public ApiKeyAuthenticator(List<ApiKeyAuthorizer> authorizers) {
        this.authorizers = authorizers;
    }

    @Override
    public boolean isAuthenticated(String authorizationHeader) {
        boolean success = apiKeyFrom(authorizationHeader)
            .map(
                key -> {
                    for (ApiKeyAuthorizer authorizer : authorizers) {
                        if (!authorizer.authorize(key)) {
                            return false;
                        }
                    }
                    return true;
                }
            )
            .orElse(false);
        if (!success) {
            logger.info("Failed to authenticate...");
        }
        return success;
    }

    private Optional<ApiKey> apiKeyFrom(String authorizationHeader) {
        return Optional.ofNullable(authorizationHeader)
            .filter(it -> it.startsWith("Bearer "))
            .map(it -> it.replaceFirst("Bearer ", ""))
            .flatMap(this::base64Decode)
            .flatMap(this::splitDecodedApiKey);
    }

    private Optional<String> base64Decode(String value) {
        try {
            byte[] decode = Base64.getDecoder().decode(value);
            return Optional.of(new String(decode, UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<ApiKey> splitDecodedApiKey(String decodedApiKey) {
        int colonPos = decodedApiKey.indexOf(":");
        if (colonPos == -1) return Optional.empty();

        String keyName = decodedApiKey.substring(0, colonPos);
        String keyValue = decodedApiKey.substring(colonPos + 1);

        if (keyName.isEmpty() || keyValue.isEmpty())
            return Optional.empty();

        return Optional.of(ApiKey.of(keyName, keyValue));
    }

    public static ApiKeyAuthenticator authenticatingWithApiKey(ApiKeyAuthorizer... authorizers) {
        return new ApiKeyAuthenticator(newArrayList(authorizers));
    }
}
