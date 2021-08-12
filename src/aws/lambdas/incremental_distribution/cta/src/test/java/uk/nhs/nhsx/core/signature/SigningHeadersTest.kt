package uk.nhs.nhsx.core.signature

import assertions.MetaHeaderAssertions.matchesMeta
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import java.time.Instant

class SigningHeadersTest {

    @Test
    fun `to headers`() {
        val headers = SigningHeaders.fromDatedSignature(
            DatedSignature(
                SignatureDate(
                    "somedate", Instant.EPOCH
                ),
                Signature(
                    KeyId.of("some-key"),
                    SigningAlgorithmSpec.ECDSA_SHA_256, byteArrayOf(48, 49, 50)
                )
            )
        )

        expectThat(headers.toList()).matchesMeta(KeyId.of("some-key"), "MDEy", "somedate")
    }
}
