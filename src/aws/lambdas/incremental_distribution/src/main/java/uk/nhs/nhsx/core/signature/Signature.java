package uk.nhs.nhsx.core.signature;

import com.amazonaws.services.kms.model.SigningAlgorithmSpec;

import java.nio.ByteBuffer;
import java.util.Base64;

public class Signature {

    public final KeyId keyId;
    public final SigningAlgorithmSpec algo;
    private final byte[] bytes;

    public Signature(KeyId keyId, SigningAlgorithmSpec algo, byte[] bytes) {
        this.keyId = keyId;
        this.algo = algo;
        this.bytes = bytes;
    }

    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(bytes);
    }

    public String asBase64Encoded() {
        return Base64.getEncoder().encodeToString(bytes);
    }

}
