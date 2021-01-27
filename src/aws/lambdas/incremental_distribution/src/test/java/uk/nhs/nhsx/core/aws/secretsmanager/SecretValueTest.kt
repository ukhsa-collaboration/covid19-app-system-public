package uk.nhs.nhsx.core.aws.secretsmanager

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class SecretValueTest {

    @Test
    fun toStringDoesntGiveAnythingAway() {
        assertThat(SecretValue.of("something").toString(), not(containsString("something")))
    }
}