package uk.nhs.nhsx.core.signature

interface DatedSigner {
    fun sign(content: (SignatureDate) -> ByteArray): DatedSignature
}
