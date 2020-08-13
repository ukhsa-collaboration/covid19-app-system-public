package uk.nhs.nhsx.core.auth;

import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.CachingSecretManager;

import java.util.regex.Pattern;

public class StandardAuthentication {
    private static final Pattern pattern = Pattern.compile("^[\\w_-]{6,50}");

    public static Authenticator awsAuthentication(ApiName apiName) {
        return ApiKeyAuthenticator.authenticatingWithApiKey(
            apiKeyNameValidator(),
            new SecretManagerAuthenticator(
                apiName,
                new CachingSecretManager(new AwsSecretManager())
            )
        );
    }

    public static ApiKeyAuthorizer apiKeyNameValidator() {
            return key -> pattern.matcher(key.keyName).matches();
    }
}
