package uk.nhs.nhsx.activationsubmission.validate;

import uk.nhs.nhsx.activationsubmission.ActivationCode;

public interface ActivationCodeValidator {
    boolean validate(ActivationCode code);

    default ActivationCodeValidator and (ActivationCodeValidator other) {
        return (c) -> this.validate(c) && other.validate(c);
    }

    default ActivationCodeValidator or (ActivationCodeValidator other) {
        return (c) -> this.validate(c) || other.validate(c);
    }
}
