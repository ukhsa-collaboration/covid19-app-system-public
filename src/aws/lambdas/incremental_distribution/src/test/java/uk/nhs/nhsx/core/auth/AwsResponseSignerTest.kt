package uk.nhs.nhsx.core.auth

import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.events.NoRequestIdFound
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import java.time.Instant
import java.util.*

class AwsResponseSignerTest {

    private val events = RecordingEvents()

    private val signature = "some-signature".toByteArray() // base64 is c29tZS1zaWduYXR1cmU=
    private val signatureResult = Signature(KeyId.of("some-id"), SigningAlgorithmSpec.ECDSA_SHA_256, signature)
    private val now = Instant.parse("2020-08-02T10:18:44.000Z")
    private val expectedSignatureDate = "Sun, 02 Aug 2020 10:18:44 UTC"

    @Test
    fun `signs a response and sets the signature date`() {
        val request = ProxyRequestBuilder
            .request()
            .withMethod(POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id")
            .build()

        val response = HttpResponses.ok("""{"foo":"bar"}""")

        val expectedContentToSign = calculateExpectedContent(
            "client-request-id",
            "POST",
            expectedSignatureDate,
            "/some/path",
            """{"foo":"bar"}"""
        )

        assert(expectedContentToSign).sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("""keyId="some-id",signature="c29tZS1zaWduYXR1cmU="""")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun `signs a response when header set lowercase and sets the signature date`() {
        val request = ProxyRequestBuilder
            .request()
            .withMethod(POST)
            .withPath("/some/path")
            .withHeader("request-id", "client-request-id")
            .build()

        val response = HttpResponses.ok("""{"foo":"bar"}""")

        val expectedContentToSign = calculateExpectedContent(
            "client-request-id",
            "POST",
            expectedSignatureDate,
            "/some/path",
            """{"foo":"bar"}"""
        )

        assert(expectedContentToSign).sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("keyId=\"some-id\",signature=\"c29tZS1zaWduYXR1cmU=\"")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun `signing some binary content`() {
        val request = ProxyRequestBuilder
            .request()
            .withMethod(POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id")
            .build()

        val response = HttpResponses.ok().apply {
            isBase64Encoded = true
            body = Base64.getEncoder().encodeToString(byteArrayOf(0, 1, 2, 3, 4, 5))
        }

        val expectedContentToSign = calculateExpectedContent(
            "client-request-id",
            "POST",
            expectedSignatureDate,
            "/some/path",
            byteArrayOf(0, 1, 2, 3, 4, 5)
        )

        assert(expectedContentToSign).sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("""keyId="some-id",signature="c29tZS1zaWduYXR1cmU="""")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun `signing a response with no content`() {
        val request = ProxyRequestBuilder
            .request()
            .withMethod(POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id")
            .build()

        val response = HttpResponses.ok()

        val expectedContentToSign = calculateExpectedContent(
            "client-request-id",
            "POST",
            "Sun, 02 Aug 2020 10:18:44 UTC",
            "/some/path",
            ""
        )
        assert(expectedContentToSign).sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("""keyId="some-id",signature="c29tZS1zaWduYXR1cmU="""")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun `signing a response when isBase64Encoded is null`() {
        val request = ProxyRequestBuilder
            .request()
            .withMethod(POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id")
            .build()

        val response = HttpResponses.ok().apply {
            isBase64Encoded = null
        }

        val expectedContentToSign = calculateExpectedContent(
            "client-request-id",
            "POST",
            "Sun, 02 Aug 2020 10:18:44 UTC",
            "/some/path",
            ""
        )
        assert(expectedContentToSign).sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("""keyId="some-id",signature="c29tZS1zaWduYXR1cmU="""")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)
    }

    @Test
    fun `missing requestId raises event`() {
        val request = ProxyRequestBuilder
            .request()
            .withMethod(POST)
            .withPath("/some/path")
            .build()

        val response = HttpResponses.ok("""{"foo":"bar"}""")

        val expectedContentToSign = calculateExpectedContent(
            "not-set",
            "POST",
            expectedSignatureDate,
            "/some/path",
            """{"foo":"bar"}"""
        )

        assert(expectedContentToSign).sign(request, response)

        assertThat(response.headers["X-Amz-Meta-Signature"]).isEqualTo("""keyId="some-id",signature="c29tZS1zaWduYXR1cmU="""")
        assertThat(response.headers["X-Amz-Meta-Signature-Date"]).isEqualTo(expectedSignatureDate)

        events.containsExactly(NoRequestIdFound::class)
    }

    private fun calculateExpectedContent(
        requestId: String,
        method: String,
        date: String,
        path: String,
        content: String
    ): ByteArray = "$requestId:$method:$path:$date:$content".toByteArray()

    private fun calculateExpectedContent(
        requestId: String,
        method: String,
        date: String,
        path: String,
        content: ByteArray
    ): ByteArray = "$requestId:$method:$path:$date:".toByteArray() + content

    private fun assert(expected: ByteArray): AwsResponseSigner {
        return AwsResponseSigner(RFC2616DatedSigner({ now }, {
            assertThat(it).isEqualTo(expected)
            signatureResult
        }), events)
    }
}
