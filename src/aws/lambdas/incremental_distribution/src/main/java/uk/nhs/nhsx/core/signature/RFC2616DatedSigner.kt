package uk.nhs.nhsx.core.signature

import uk.nhs.nhsx.core.signature.DatedSignature.SignatureDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Function
import java.util.function.Supplier

class RFC2616DatedSigner(private val clock: Supplier<Instant>, private val signer: Signer) : DatedSigner {
    private val format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)

    override fun sign(content: Function<SignatureDate, ByteArray>): DatedSignature {
        val instant = clock.get()
        val date = format.format(instant.atZone(ZoneId.of("UTC")))
        val signatureDate = SignatureDate(date, instant)
        return DatedSignature(signatureDate, signer.sign(content.apply(signatureDate)))
    }
}
