package uk.nhs.nhsx.core.aws.ssm

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.signature.KeyId

class KeyIdTest {

    @Test
    fun testKeyId() {
        assertThat(
            KeyId.of("arn:aws:kms:eu-west-2:1234567890:key/b4c27bf3-8a76-4d2b-b91c-2152e7710a57").value,
            equalTo("b4c27bf3-8a76-4d2b-b91c-2152e7710a57")
        )
        assertThat(
            KeyId.of("b4c27bf3-8a76-4d2b-b91c-2152e7710a57").value,
            equalTo("b4c27bf3-8a76-4d2b-b91c-2152e7710a57")
        )
    }
}