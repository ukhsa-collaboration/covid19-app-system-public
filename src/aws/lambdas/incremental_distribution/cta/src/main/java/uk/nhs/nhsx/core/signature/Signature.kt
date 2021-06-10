package uk.nhs.nhsx.core.signature

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.kms.model.SigningAlgorithmSpec.ECDSA_SHA_256
import uk.nhs.nhsx.core.exceptions.Defect
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Base64

class Signature(val keyId: KeyId, private val algo: SigningAlgorithmSpec, bytes: ByteArray) {
    private val bytes: ByteArray = bytes.copyOf(bytes.size)

    fun asByteBuffer(): ByteBuffer = ByteBuffer.wrap(bytes)

    fun asBase64Encoded(): String = Base64.getEncoder().encodeToString(bytes)

    /*
        Sun & KMS ECDSA signature implementation produces and expects a DER encoding
        of R and S while JWS wants R and S as a concatenated byte array
     */
    fun asJWSCompatible(): ByteArray = when (algo) {
        ECDSA_SHA_256 -> try {
            DerToConcatenated.convertDerToConcatenated(bytes, 64)
        } catch (e: IOException) {
            throw Defect("Signature format incorrect", e)
        }
        else -> throw Defect("only implemented for SigningAlgorithmSpec.ECDSA_SHA_256")
    }
}
