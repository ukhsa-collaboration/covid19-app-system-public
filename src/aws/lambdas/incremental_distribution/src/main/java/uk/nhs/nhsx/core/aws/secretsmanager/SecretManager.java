package uk.nhs.nhsx.core.aws.secretsmanager;

import java.util.Optional;

public interface SecretManager {
    Optional<SecretValue> getSecret(SecretName secretName);
    byte[] getSecretBinary(SecretName secretName);
}