package uk.nhs.nhsx.activationsubmission;

import org.junit.Test;
import uk.nhs.nhsx.activationsubmission.persist.ActivationCodeBatchName;
import uk.nhs.nhsx.activationsubmission.persist.PersistedActivationCode;
import uk.nhs.nhsx.activationsubmission.validate.ActivationCodeValidator;
import uk.nhs.nhsx.activationsubmission.validate.PersistedActivationCodeValidator;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PersistedActivationCodeValidatorTest {

    @Test
    public void codeThatCannotBeFoundIsInvalid() throws Exception {
        ActivationCodeValidator validator = new PersistedActivationCodeValidator(code -> Optional.empty());
        assertThat(validator.validate(ActivationCode.of("1234")), is(false));
    }

    @Test
    public void codeThatCanBeFoundIsValid() throws Exception {
        ActivationCodeValidator validator = new PersistedActivationCodeValidator(code -> Optional.of(new PersistedActivationCode(Instant.now(), ActivationCodeBatchName.of("batch"), ActivationCode.of("1234"), Instant.now(), false)));
        assertThat(validator.validate(ActivationCode.of("1234")), is(true));
    }
}