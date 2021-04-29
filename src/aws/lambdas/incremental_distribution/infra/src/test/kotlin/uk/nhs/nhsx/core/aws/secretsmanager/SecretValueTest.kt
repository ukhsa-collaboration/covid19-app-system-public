package uk.nhs.nhsx.core.aws.secretsmanager

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test

class SecretValueTest {

    @Test
    fun toStringDoesntGiveAnythingAway() {
        MatcherAssert.assertThat(
            SecretValue.of("something").toString(),
            CoreMatchers.not(CoreMatchers.containsString("something"))
        )
    }
}
