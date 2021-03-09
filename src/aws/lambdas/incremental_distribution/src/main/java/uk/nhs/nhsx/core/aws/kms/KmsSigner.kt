package uk.nhs.nhsx.core.aws.kms

import uk.nhs.nhsx.core.signature.KeyId
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.MessageType
import com.amazonaws.services.kms.model.SignRequest
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import uk.nhs.nhsx.core.exceptions.Defect
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import java.nio.ByteBuffer
import java.util.function.Supplier

class KmsSigner(
    private val keyId: Supplier<KeyId>,
    private val kmsClient: AWSKMS
) : Signer {

    override fun sign(bytes: ByteArray): Signature {
        val hash = hashOrThrow(bytes)
        val keyId = keyId.get()
        val signResult = kmsClient.sign(
            SignRequest()
                .withKeyId(keyId.value)
                .withMessage(ByteBuffer.wrap(hash))
                .withMessageType(MessageType.DIGEST)
                .withSigningAlgorithm(algorithm)
        )
        return Signature(
            keyId,
            algorithm,
            signResult.signature.array()
        )
    }

    private fun hashOrThrow(content: ByteArray): ByteArray = try {
        MessageDigest.getInstance("SHA-256").digest(content)
    } catch (e: NoSuchAlgorithmException) {
        throw Defect("Failed to digest message", e)
    }

    companion object {
        private val algorithm = SigningAlgorithmSpec.ECDSA_SHA_256
    }
}
