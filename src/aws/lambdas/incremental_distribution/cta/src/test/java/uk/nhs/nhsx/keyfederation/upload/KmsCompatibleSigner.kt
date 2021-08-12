package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.kms.model.SigningAlgorithmSpec.ECDSA_SHA_256
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import java.security.PrivateKey
import java.security.Signature as JavaSignature

class KmsCompatibleSigner(
    val key: PrivateKey,
    private val claimedSignatureAlgo: SigningAlgorithmSpec = ECDSA_SHA_256
) : Signer {
    override fun sign(bytes: ByteArray): Signature {
        val signature = JavaSignature.getInstance("SHA256withECDSA").apply {
            initSign(key)
            update(bytes)
        }.sign()

        return Signature(
            keyId = KeyId.of("arn"),
            algo = claimedSignatureAlgo,
            bytes = signature
        )
    }
}
