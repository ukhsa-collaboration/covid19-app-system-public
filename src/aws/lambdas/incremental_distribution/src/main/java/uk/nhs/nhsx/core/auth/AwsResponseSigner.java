package uk.nhs.nhsx.core.auth;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.signature.DatedSignature;
import uk.nhs.nhsx.core.signature.DatedSigner;
import uk.nhs.nhsx.core.signature.SigningHeaders;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class AwsResponseSigner implements ResponseSigner {

    private final Logger logger = LogManager.getLogger(AwsResponseSigner.class);

    private final DatedSigner contentSigner;

    public AwsResponseSigner(DatedSigner signer) {
        this.contentSigner = signer;
    }

    @Override
    public void sign(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) {
        String responseContent = Optional.ofNullable(response.getBody()).orElse("");

        String requestId = Optional.ofNullable(request.getHeaders().get("Request-Id"))
                .orElseGet(() -> {
                    logger.warn("Unable to find value for request-id, using default. Signature will be wrong");
                    return "not-set";
                });

        String method = request.getHttpMethod();
        String path = request.getPath();

        DatedSignature signature = contentSigner.sign(signatureDate -> {
            if (Optional.ofNullable(response.getIsBase64Encoded()).orElse(false)) {
                byte[] nonContentBytes = String.format("%s:%s:%s:%s:", requestId, method, path, signatureDate.string).getBytes(StandardCharsets.UTF_8);
                byte[] contentBytes = Base64.decode(responseContent);

                ByteBuffer buffer = ByteBuffer.allocate(nonContentBytes.length + contentBytes.length);

                buffer.put(nonContentBytes);
                buffer.put(contentBytes);

                return buffer.array();
            } else {
                return String.format("%s:%s:%s:%s:%s", requestId, method, path, signatureDate.string, responseContent).getBytes(StandardCharsets.UTF_8);
            }
        });

        Map<String, String> headers = response.getHeaders();

        Arrays.stream(SigningHeaders.fromDatedSignature(signature)).forEach(
            header -> headers.put(header.asHttpHeaderName(), header.value)
        );
    }
}
