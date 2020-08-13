package uk.nhs.nhsx.core.aws.secretsmanager;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class SecretValueTest {

    @Test
    public void toStringDoesntGiveAnythingAway() throws Exception {
        assertThat(SecretValue.of("something").toString(), not(containsString("something")));
    }
}