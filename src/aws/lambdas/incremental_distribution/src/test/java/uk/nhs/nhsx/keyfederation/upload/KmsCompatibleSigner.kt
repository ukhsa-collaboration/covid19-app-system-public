package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import java.security.PrivateKey

class KmsCompatibleSigner(val key: PrivateKey, val claimedSignatureAlgo: SigningAlgorithmSpec = SigningAlgorithmSpec.ECDSA_SHA_256) : Signer {
    override fun sign(bytes: ByteArray): Signature = java.security.Signature.getInstance("SHA256withECDSA").let {
        it.initSign(key)
        it.update(bytes)
        Signature(
            KeyId.of("arn"),
            claimedSignatureAlgo,
            it.sign()
        )
    }
}