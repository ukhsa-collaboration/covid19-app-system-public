package uk.nhs.nhsx.core.aws.ssm

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.testhelper.assertions.isSameAs

class KeyIdTest {

    @Test
    fun `extracts key`() {
        val keyId = KeyId.of("b4c27bf3-8a76-4d2b-b91c-2152e7710a57")
        expectThat(keyId).isSameAs("b4c27bf3-8a76-4d2b-b91c-2152e7710a57")
    }

    @Test
    fun `extracts key if from ARN`() {
        val keyId = KeyId.of("arn:aws:kms:eu-west-2:1234567890:key/b4c27bf3-8a76-4d2b-b91c-2152e7710a57")
        expectThat(keyId).isSameAs("b4c27bf3-8a76-4d2b-b91c-2152e7710a57")
    }
}
