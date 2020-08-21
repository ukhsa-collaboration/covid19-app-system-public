package uk.nhs.nhsx.activationsubmission;

import org.junit.Test;
import uk.nhs.nhsx.activationsubmission.validate.ActivationCodeValidator;
import uk.nhs.nhsx.activationsubmission.validate.CrockfordActivationCodeValidator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CrockfordActivationCodeValidatorTest {

    ActivationCodeValidator validator = new CrockfordActivationCodeValidator();

    @Test
    public void validatesACorrectCode() throws Exception {
        assertThat(validator.validate(ActivationCode.of("f3dzcfdt")), is(true));
        assertThat(validator.validate(ActivationCode.of("8vb7xehg")), is(true));
    }

    @Test
    public void doesNotValidateAnEmptyCode() throws Exception {
        assertThat(validator.validate(ActivationCode.of("")), is(false));
    }

    @Test
    public void doesNotValidateAUnicode() throws Exception {
        assertThat(validator.validate(ActivationCode.of("∞")), is(false));
    }

    @Test
    public void doesNotValidateIncorrectCode() throws Exception {
        assertThat(validator.validate(ActivationCode.of("f3dzcfdx")), is(false));
        assertThat(validator.validate(ActivationCode.of("8vb7xehb")), is(false));
    }
}
