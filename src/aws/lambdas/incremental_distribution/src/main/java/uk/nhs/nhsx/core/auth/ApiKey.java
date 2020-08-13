package uk.nhs.nhsx.core.auth;

import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;

public class ApiKey {
    public final String keyName;
    public final String keyValue;

    private ApiKey(String keyName, String keyValue) {
        this.keyName = keyName;
        this.keyValue = keyValue;
    }

    public static ApiKey of(String keyName, String keyValue) {
        return new ApiKey(keyName, keyValue);
    }

    public SecretName secretNameFrom(ApiName apiName) {
        return SecretName.of("/" + apiName.name + "/" + keyName);
    }
}
