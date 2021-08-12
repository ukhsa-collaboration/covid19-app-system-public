package uk.nhs.nhsx.testhelper

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import uk.nhs.nhsx.core.signature.DatedSignature
import uk.nhs.nhsx.core.signature.DatedSigner
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.SignatureDate
import java.time.Instant

class TestDatedSigner(private val date: String) : DatedSigner {

    private val signatureBytes = byteArrayOf(0, 1, 2, 3, 4)
    var content: MutableList<ByteArray> = ArrayList()
    var count = 0
    var keyId: KeyId = KeyId.of("some-key")

    override fun sign(content: (SignatureDate) -> ByteArray): DatedSignature {
        count++
        val date = SignatureDate(date, Instant.EPOCH)
        this.content.add(content(date))
        return DatedSignature(date, Signature(keyId, SigningAlgorithmSpec.ECDSA_SHA_256, signatureBytes))
    }
}
