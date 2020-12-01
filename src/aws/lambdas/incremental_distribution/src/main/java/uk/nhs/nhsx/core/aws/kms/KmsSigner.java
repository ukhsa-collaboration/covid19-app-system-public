package uk.nhs.nhsx.core.aws.kms;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.MessageType;
import com.amazonaws.services.kms.model.SignRequest;
import com.amazonaws.services.kms.model.SignResult;
import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import uk.nhs.nhsx.core.exceptions.Defect;
import uk.nhs.nhsx.core.signature.KeyId;
import uk.nhs.nhsx.core.signature.Signature;
import uk.nhs.nhsx.core.signature.Signer;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

public class KmsSigner implements Signer {

    private static final SigningAlgorithmSpec algorithm = SigningAlgorithmSpec.ECDSA_SHA_256;

    private static AWSKMS kmsClient;

    private final Supplier<KeyId> keyId;

    public KmsSigner(Supplier<KeyId> keyId) {
        this.keyId = keyId;
    }

    private static synchronized AWSKMS kmsClient() {
        if (kmsClient == null) {
            kmsClient = AWSKMSClientBuilder.defaultClient();
        }
        return kmsClient;
    }

    @Override
    public Signature sign(byte[] content) {
        byte[] hash = hashOrThrow(content);

        KeyId keyId = this.keyId.get();

        SignResult signResult = kmsClient().sign(new SignRequest()
            .withKeyId(keyId.value)
            .withMessage(ByteBuffer.wrap(hash))
            .withMessageType(MessageType.DIGEST)
            .withSigningAlgorithm(algorithm));

        return new Signature(
            keyId,
            algorithm,
            signResult.getSignature().array()
        );
    }

    private byte[] hashOrThrow(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(content);
        } catch (NoSuchAlgorithmException e) {
            throw new Defect("Failed to digest message", e);
        }
    }
}
