package uk.nhs.nhsx.activationsubmission.persist;

import uk.nhs.nhsx.core.ValueType;

public class ActivationCodeBatchName extends ValueType<ActivationCodeBatchName> {

    private ActivationCodeBatchName(String value) {
        super(value);
    }

    public static ActivationCodeBatchName of(String name) {
        return new ActivationCodeBatchName(name);
    }
}
