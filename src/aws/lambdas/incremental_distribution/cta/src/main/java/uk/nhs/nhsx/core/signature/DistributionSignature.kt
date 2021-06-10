package uk.nhs.nhsx.core.signature

import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import java.io.ByteArrayOutputStream

class DistributionSignature(private val bytes: ByteArraySource) : (SignatureDate) -> ByteArray {
    override fun invoke(signatureDate: SignatureDate): ByteArray = ByteArrayOutputStream().use {
        it.write("${signatureDate.string}:".toByteArray())
        it.write(bytes.toArray())
        it.toByteArray()
    }
}
