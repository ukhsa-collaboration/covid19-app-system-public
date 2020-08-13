package uk.nhs.nhsx.activationsubmission.validate;

import uk.nhs.nhsx.activationsubmission.ActivationCode;
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator;

public class CrockfordActivationCodeValidator implements ActivationCodeValidator {

    private CrockfordDammRandomStringGenerator.DammChecksum checksum = CrockfordDammRandomStringGenerator.checksum();

    @Override
    public boolean validate(ActivationCode code) {
        return checksum.validate(code.value);
    }
}
