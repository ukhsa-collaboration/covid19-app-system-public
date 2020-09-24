package uk.nhs.nhsx.core.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class SecretManagerKeyAuthorizer implements ApiKeyAuthorizer {
    private static final Logger logger = LogManager.getLogger(SecretManagerKeyAuthorizer.class);

    private final SecretManager secretManager;
    private final ApiName apiName;

    public SecretManagerKeyAuthorizer(ApiName apiName, SecretManager secretManager) {
        this.secretManager = secretManager;
        this.apiName = apiName;
    }

    @Override
    public boolean authorize(ApiKey apiKey) {
        SecretName secretName = apiKey.secretNameFrom(apiName);
        Optional<SecretValue> secret1 = secretManager.getSecret(secretName);
        return secret1
            .map(secret -> verifyKeyValue(apiKey, secret))
            .orElse(false);
    }

    private boolean verifyKeyValue(ApiKey apiKey, SecretValue secret) {
        boolean verified = BCrypt.verifyer()
            .verify(apiKey.keyValue.getBytes(StandardCharsets.UTF_8), secret.value.getBytes(StandardCharsets.UTF_8))
            .verified;

        if (!verified) {
            logger.warn(
                "Verification of keyName:" + apiKey.keyName + " failed. " +
                    "Secret provider in api call did not match."
            );
        }

        return verified;
    }
}
