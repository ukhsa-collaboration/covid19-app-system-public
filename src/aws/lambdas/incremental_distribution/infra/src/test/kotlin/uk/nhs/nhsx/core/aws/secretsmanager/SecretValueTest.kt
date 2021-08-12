package uk.nhs.nhsx.core.aws.secretsmanager

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

class SecretValueTest {

    @Test
    fun `to string doesnt give anything away`() {
        expectThat(SecretValue.of("something").toString()).not().contains("something")
    }
}
