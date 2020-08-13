package uk.nhs.nhsx.core.auth;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.signature.KeyId;
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner;
import uk.nhs.nhsx.core.signature.Signature;
import uk.nhs.nhsx.core.signature.Signer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

public class AwsResponseSignerTest {


    private final byte[] signature = "some-signature".getBytes(StandardCharsets.UTF_8); // base64 is c29tZS1zaWduYXR1cmU=

    private final Signature signatureResult = new Signature(KeyId.of("some-id"), SigningAlgorithmSpec.ECDSA_SHA_256, signature);

    private byte[] expectedContentToSign = new byte[0];

    private final Signer contentSigner = b -> {
        // note that printed strings might look the same, but may be different bytes due to encodings...
        String actual = new String(b, StandardCharsets.UTF_8);
        String expected = new String(expectedContentToSign, StandardCharsets.UTF_8);
        MatcherAssert.assertThat(String.format("Expecting to be signing:\n'%s'\nGot:\n'%s'", expected, actual), b, equalTo(expectedContentToSign));
        return signatureResult;
    };

    ZonedDateTime now = ZonedDateTime.of(
        LocalDate.of(2020, Month.AUGUST, 2),
        LocalTime.of(10, 18, 44),
        ZoneId.of("UTC")
    );

    private final AwsResponseSigner signer = new AwsResponseSigner(new RFC2616DatedSigner(() -> now.toInstant(), contentSigner));

    private final ProxyRequestBuilder requestBuilder = ProxyRequestBuilder.request();

    @Test
    public void signsAResponseAndSetsTheSignatureDate() {

        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id");

        APIGatewayProxyRequestEvent request = requestBuilder.build();

        APIGatewayProxyResponseEvent response = HttpResponses.ok("{\"foo\":\"bar\"}");
        String expectedSignatureDate = "Sun, 02 Aug 2020 10:18:44 UTC";

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", expectedSignatureDate, "/some/path", "{\"foo\":\"bar\"}");

        signer.sign(request, response);
        assertThat(response.getHeaders().get("X-Amz-Meta-Signature")).isEqualTo("keyId=\"some-id\",signature=\"c29tZS1zaWduYXR1cmU=\"");
        assertThat(response.getHeaders().get("X-Amz-Meta-Signature-Date")).isEqualTo(expectedSignatureDate);
    }

    @Test
    public void signsAResponseWhenHeaderSetLowerCaseAndSetsTheSignatureDate() {

        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("request-id", "client-request-id");

        APIGatewayProxyRequestEvent request = requestBuilder.build();

        APIGatewayProxyResponseEvent response = HttpResponses.ok("{\"foo\":\"bar\"}");
        String expectedSignatureDate = "Sun, 02 Aug 2020 10:18:44 UTC";

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", expectedSignatureDate, "/some/path", "{\"foo\":\"bar\"}");

        signer.sign(request, response);
        assertThat(response.getHeaders().get("X-Amz-Meta-Signature")).isEqualTo("keyId=\"some-id\",signature=\"c29tZS1zaWduYXR1cmU=\"");
        assertThat(response.getHeaders().get("X-Amz-Meta-Signature-Date")).isEqualTo(expectedSignatureDate);
    }

    @Test
    public void signingSomeBinaryContent() throws Exception {

        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id");

        APIGatewayProxyRequestEvent request = requestBuilder.build();

        byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5};
        String encodedBody = Base64.getEncoder().encodeToString(bytes);

        String expectedSignatureDate = "Sun, 02 Aug 2020 10:18:44 UTC";

        APIGatewayProxyResponseEvent response = HttpResponses.ok();

        response.setIsBase64Encoded(true);
        response.setBody(encodedBody);

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", expectedSignatureDate, "/some/path", bytes);

        signer.sign(request, response);
    }

    @Test
    public void signingAResponseWithNoContent() {

        requestBuilder.withMethod(HttpMethod.POST)
            .withPath("/some/path")
            .withHeader("Request-Id", "client-request-id");

        APIGatewayProxyResponseEvent response = HttpResponses.ok();

        expectedContentToSign = calculateExpectedContent("client-request-id", "POST", "Sun, 02 Aug 2020 10:18:44 UTC", "/some/path", "");

        signer.sign(requestBuilder.build(), response);
        assertThat(response.getHeaders()).containsKey("X-Amz-Meta-Signature");
    }

    private byte[] calculateExpectedContent(String requestId, String method, String date, String path, String content) {
        return String.format("%s:%s:%s:%s:%s", requestId, method, path, date, content).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] calculateExpectedContent(String requestId, String method, String date, String path, byte[] content) {

        byte[] nonContentBytes = String.format("%s:%s:%s:%s:", requestId, method, path, date).getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(nonContentBytes.length + content.length);

        buffer.put(nonContentBytes);
        buffer.put(content);

        return buffer.array();
    }
}
