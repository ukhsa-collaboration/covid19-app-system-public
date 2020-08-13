package uk.nhs.nhsx.core.aws.secretsmanager;

import uk.nhs.nhsx.core.ValueType;

public class SecretValue extends ValueType<SecretValue> {

    private SecretValue(String value) {
        super(value);
    }

    public static SecretValue of(String secret) {
        return new SecretValue(secret);
    }

    @Override
    public String toString() {
        return "Secret{secret=****}";
    }
}
