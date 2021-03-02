package uk.nhs.nhsx.core.signature

import uk.nhs.nhsx.core.signature.DatedSignature.SignatureDate
import java.util.function.Function

interface DatedSigner {
    fun sign(content: Function<SignatureDate, ByteArray>): DatedSignature
}
