package uk.nhs.nhsx.core.auth

import com.amazonaws.HttpMethod
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.*
import java.util.*

class AwsResponseSignerTest {

    private val signature = "some-signature".toByteArray(StandardCharsets.UTF_8) // base64 is c29tZS1zaWduYXR1cmU=
    private val signatureResult = Signature(KeyId.of("some-id"), SigningAlgorithmSpec.ECDSA_SHA_256, signature)
    private var expectedContentToSign = ByteArray(0)
    private val contentSigner = Signer { b: ByteArray ->
        // note that printed strings might look the same, but may be different bytes due to encodings...
        val actual = String(b, StandardCharsets.UTF_8)
        val expected = String(expectedContentToSign, StandardCharsets.UTF_8)
        assertThat(String.format("Expecting to be signing:\n'%s'\nGot:\n'%s'", expected, actual), b, equalTo(expectedContentToSign))
        signatureResult
    }
    private val now = ZonedDateTime.of(
        LocalDate.of(2020, Month.AUGUST, 2),
        LocalTime.of(10, 18, 44),
        ZoneId.of("UTC")
    )
    private val expectedSignatureDate = "Sun, 02 Aug 2020 10:18:44 UTC"
    private val signer = AwsResponseSigner(RFC2616DatedSigner({ now.toInstant() }, contentSigner))
    private val requestBuilder = ProxyRequestBuilder.request()

    @Test
    fun signsAResponseAndSetsTheSignatureDate() {
        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id")
        val request = requestBuilder.build()
        val response = HttpResponses.ok("{\"foo\":\"bar\"}")

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", expectedSignatureDate, "/some/path", "{\"foo\":\"bar\"}")
        signer.sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("keyId=\"some-id\",signature=\"c29tZS1zaWduYXR1cmU=\"")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun signsAResponseWhenHeaderSetLowerCaseAndSetsTheSignatureDate() {
        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("request-id", "client-request-id")
        val request = requestBuilder.build()
        val response = HttpResponses.ok("{\"foo\":\"bar\"}")

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", expectedSignatureDate, "/some/path", "{\"foo\":\"bar\"}")
        signer.sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("keyId=\"some-id\",signature=\"c29tZS1zaWduYXR1cmU=\"")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun signingSomeBinaryContent() {
        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id")
        val request = requestBuilder.build()
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val encodedBody = Base64.getEncoder().encodeToString(bytes)
        val response = HttpResponses.ok()
        response.isBase64Encoded = true
        response.body = encodedBody

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", expectedSignatureDate, "/some/path", bytes)
        signer.sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("keyId=\"some-id\",signature=\"c29tZS1zaWduYXR1cmU=\"")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun signingAResponseWithNoContent() {
        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id")
        val response = HttpResponses.ok()

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", "Sun, 02 Aug 2020 10:18:44 UTC", "/some/path", "")
        signer.sign(requestBuilder.build(), response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("keyId=\"some-id\",signature=\"c29tZS1zaWduYXR1cmU=\"")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    private fun calculateExpectedContent(requestId: String, method: String, date: String, path: String, content: String): ByteArray {
        return String.format("%s:%s:%s:%s:%s", requestId, method, path, date, content).toByteArray(StandardCharsets.UTF_8)
    }

    private fun calculateExpectedContent(requestId: String, method: String, date: String, path: String, content: ByteArray): ByteArray {
        val nonContentBytes = String.format("%s:%s:%s:%s:", requestId, method, path, date).toByteArray(StandardCharsets.UTF_8)
        val buffer = ByteBuffer.allocate(nonContentBytes.size + content.size)
        buffer.put(nonContentBytes)
        buffer.put(content)
        return buffer.array()
    }
}