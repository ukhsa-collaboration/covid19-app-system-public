package uk.nhs.nhsx.core.signature

import uk.nhs.nhsx.core.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RFC2616DatedSigner(private val clock: Clock, private val signer: Signer) : DatedSigner {
    private val format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)

    override fun sign(content: (SignatureDate) -> ByteArray): DatedSignature {
        val instant = clock()
        val date = format.format(instant.atZone(ZoneId.of("UTC")))
        val signatureDate = SignatureDate(date, instant)
        return DatedSignature(signatureDate, signer.sign(content(signatureDate)))
    }
}
