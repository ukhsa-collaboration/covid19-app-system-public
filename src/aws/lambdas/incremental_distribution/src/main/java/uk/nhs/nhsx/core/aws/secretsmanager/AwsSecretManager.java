package uk.nhs.nhsx.core.aws.secretsmanager;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;

import java.util.Optional;

public class AwsSecretManager implements SecretManager {

    private final AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
            .build();

    @Override
    public Optional<SecretValue> getSecret(SecretName secretName) {
        try {
            GetSecretValueResult getSecretValueResult =
                client.getSecretValue(new GetSecretValueRequest().withSecretId(secretName.value));

            return Optional
                .ofNullable(getSecretValueResult.getSecretString())
                .map(SecretValue::of);

        } catch (AWSSecretsManagerException e) {
            return Optional.empty();
        }
    }
}
