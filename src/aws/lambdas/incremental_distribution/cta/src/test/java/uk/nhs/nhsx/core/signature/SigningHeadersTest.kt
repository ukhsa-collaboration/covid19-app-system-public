package uk.nhs.nhsx.core.signature

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import java.time.Instant

class SigningHeadersTest {

    @Test
    fun toHeaders() {
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
        assertThat(headers.toList(), matchesMeta(KeyId.of("some-key"), "MDEy", "somedate"))
    }

    companion object {
        fun matchesMeta(keyId: KeyId, signature: String, date: String): Matcher<List<MetaHeader>> {
            return object : TypeSafeDiagnosingMatcher<List<MetaHeader>>() {
                override fun matchesSafely(headers: List<MetaHeader>, description: Description): Boolean {
                    assertThat(headers.size, equalTo(2))
                    assertThat(headers[0].asS3MetaName(), equalTo("Signature"))
                    assertThat(headers[0].value, equalTo(String.format("keyId=\"%s\",signature=\"%s\"", keyId.value, signature)))
                    assertThat(headers[1].asS3MetaName(), equalTo("Signature-Date"))
                    assertThat(headers[1].value, equalTo(date))
                    return true
                }

                override fun describeTo(description: Description) {
                    description.appendText("matches expected headers for a dated signature")
                }
            }
        }
    }
}
