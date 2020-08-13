package uk.nhs.nhsx.activationsubmission.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.activationsubmission.ActivationCode;

public class DevelopmentEnvironmentActivationValidator implements ActivationCodeValidator {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentEnvironmentActivationValidator.class);

    @Override
    public boolean validate(ActivationCode code) {
        if ( code.value.length() == 8 ) {
            boolean startsWith = code.value.startsWith("a");
            if ( startsWith ) {
                log.warn("Special case validating code " + code + " as acceptable");
                return true;
            }
        }
        return false;
    }
}
