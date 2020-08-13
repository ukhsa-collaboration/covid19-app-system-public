package uk.nhs.nhsx.core.aws.secretsmanager;

import uk.nhs.nhsx.core.ValueType;

public class SecretName extends ValueType<SecretName> {
    private SecretName(String value) {
        super(value);
    }

    public static SecretName of(String name) {
        return new SecretName(name);
    }
}
