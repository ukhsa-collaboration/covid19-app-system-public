package uk.nhs.nhsx.keyfederation.upload;

import uk.nhs.nhsx.core.signature.Signature;
import uk.nhs.nhsx.core.signature.Signer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JWS {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder();
    
    private final Signer signer;

    public JWS(Signer signer) {
        this.signer = signer;
    }

    public String sign(String payload) {

        String encodedHeader = encode("{\"alg\":\"ES256\"}");
        String encodedPayload = encode(payload);

        String signedComponent = encodedHeader + "." + encodedPayload;

        Signature signature = signer.sign(signedComponent.getBytes(StandardCharsets.UTF_8));

        String encodedSignature = URL_ENCODER.encodeToString(signature.asJWSCompatible());

        return signedComponent + "." + encodedSignature;
    }

    private String encode(String string) {
        return URL_ENCODER.encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }
}
