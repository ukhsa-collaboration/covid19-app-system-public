package uk.nhs.nhsx.core.signature;

import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import uk.nhs.nhsx.core.exceptions.Defect;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

public class Signature {

    public final KeyId keyId;
    public final SigningAlgorithmSpec algo;
    private final byte[] bytes;

    public Signature(KeyId keyId, SigningAlgorithmSpec algo, byte[] bytes) {
        this.keyId = keyId;
        this.algo = algo;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(bytes);
    }

    public String asBase64Encoded() {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /*
        Sun & KMS ECDSA signature implementation produces and expects a DER encoding
        of R and S while JWS wants R and S as a concatenated byte array
     */
    public byte[] asJWSCompatible() {
        if (algo != SigningAlgorithmSpec.ECDSA_SHA_256) {
            throw new Defect("only implemented for SigningAlgorithmSpec.ECDSA_SHA_256");
        }
        try {
            return convertDerToConcatenated(bytes, 64);
        } catch (IOException e) {
            throw new Defect("Signature format incorrect", e);
        }
    }

    // From Apache-2 Licenced org.jose4j.jws.EcdsaUsingShaAlgorithm
    // Convert the DER encoding of R and S into a concatenation of R and S
    private byte[] convertDerToConcatenated(byte[] derEncodedBytes, int outputLength) throws IOException {
        if (derEncodedBytes.length < 8 || derEncodedBytes[0] != 48) {
            throw new IOException("Invalid format of ECDSA signature");
        }

        int offset;
        if (derEncodedBytes[1] > 0) {
            offset = 2;
        } else if (derEncodedBytes[1] == (byte) 0x81) {
            offset = 3;
        } else {
            throw new IOException("Invalid format of ECDSA signature");
        }

        byte rLength = derEncodedBytes[offset + 1];

        int i;
        for (i = rLength; (i > 0) && (derEncodedBytes[(offset + 2 + rLength) - i] == 0); i--) ;

        byte sLength = derEncodedBytes[offset + 2 + rLength + 1];

        int j;
        for (j = sLength; (j > 0) && (derEncodedBytes[(offset + 2 + rLength + 2 + sLength) - j] == 0); j--) ;

        int rawLen = Math.max(i, j);
        rawLen = Math.max(rawLen, outputLength / 2);

        if ((derEncodedBytes[offset - 1] & 0xff) != derEncodedBytes.length - offset
            || (derEncodedBytes[offset - 1] & 0xff) != 2 + rLength + 2 + sLength
            || derEncodedBytes[offset] != 2
            || derEncodedBytes[offset + 2 + rLength] != 2) {
            throw new IOException("Invalid format of ECDSA signature");
        }

        byte[] concatenatedSignatureBytes = new byte[2 * rawLen];

        System.arraycopy(derEncodedBytes, (offset + 2 + rLength) - i, concatenatedSignatureBytes, rawLen - i, i);
        System.arraycopy(derEncodedBytes, (offset + 2 + rLength + 2 + sLength) - j, concatenatedSignatureBytes, 2 * rawLen - j, j);

        return concatenatedSignatureBytes;
    }


}
