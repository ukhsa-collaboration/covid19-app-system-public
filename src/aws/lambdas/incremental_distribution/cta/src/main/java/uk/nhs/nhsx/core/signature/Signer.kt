package uk.nhs.nhsx.core.signature

fun interface Signer {
    fun sign(bytes: ByteArray): Signature
}
