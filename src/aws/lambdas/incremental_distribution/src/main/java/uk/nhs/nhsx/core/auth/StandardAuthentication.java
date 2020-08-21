package uk.nhs.nhsx.core.auth;

import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.CachingSecretManager;

import java.util.regex.Pattern;

import static uk.nhs.nhsx.core.auth.ApiKeyAuthenticator.authenticatingWithApiKey;
import static uk.nhs.nhsx.core.aws.xray.Tracing.tracing;

public class StandardAuthentication {
    private static final Pattern pattern = Pattern.compile("^[\\w_-]{6,50}");

    public static Authenticator awsAuthentication(ApiName apiName) {
        return authenticatingWithApiKey(
            apiKeyNameValidator(),
            tracing("authentication", ApiKeyAuthorizer.class,
                new CachingApiKeyAuthorizer(
                    new SecretManagerKeyAuthorizer(
                        apiName,
                        new CachingSecretManager(new AwsSecretManager())
                    )
                )
            )
        );
    }

    public static ApiKeyAuthorizer apiKeyNameValidator() {
        return key -> pattern.matcher(key.keyName).matches();
    }
}
