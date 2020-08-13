package uk.nhs.nhsx.activationsubmission;

import uk.nhs.nhsx.core.ValueType;

public class ActivationCode extends ValueType<ActivationCode> {

    private ActivationCode(String value) {
        super(value);
    }

    public static ActivationCode of(String key) {
        return new ActivationCode(key);
    }
}
