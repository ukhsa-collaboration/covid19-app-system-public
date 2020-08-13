package uk.nhs.nhsx.activationsubmission.validate;

import uk.nhs.nhsx.activationsubmission.ActivationCode;
import uk.nhs.nhsx.activationsubmission.persist.PersistedActivationCodeLookup;

public class PersistedActivationCodeValidator implements ActivationCodeValidator {

    private final PersistedActivationCodeLookup codes;

    public PersistedActivationCodeValidator(PersistedActivationCodeLookup codes) {
        this.codes = codes;
    }

    @Override
    public boolean validate(ActivationCode code) {
        return codes.find(code).isPresent();
    }
}
