package uk.nhs.nhsx.isolationpayment

import uk.nhs.nhsx.domain.IpcTokenId
import java.security.SecureRandom
import java.util.*

fun interface IpcTokenIdGenerator {
    fun nextToken(): IpcTokenId
}

class RandomIpcTokenIdGenerator(private val random: Random = SecureRandom()) : IpcTokenIdGenerator {
    override fun nextToken() = ByteArray(32)
        .also { random.nextBytes(it) }
        .let { it.joinToString(separator = "") { byte -> String.format("%02x", byte) } }
        .let(IpcTokenId::of)
}
