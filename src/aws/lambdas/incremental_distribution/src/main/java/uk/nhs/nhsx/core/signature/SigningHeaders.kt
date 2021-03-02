package uk.nhs.nhsx.core.signature

import uk.nhs.nhsx.core.aws.s3.MetaHeader

object SigningHeaders {
    @JvmStatic
    fun fromDatedSignature(dated: DatedSignature): List<MetaHeader> =
        listOf(from(dated.signature), MetaHeader("Signature-Date", dated.signatureDate.string))

    private fun from(signature: Signature): MetaHeader = MetaHeader(
        "Signature",
        """keyId="${signature.keyId.value}",signature="${signature.asBase64Encoded()}""""
    )
}
