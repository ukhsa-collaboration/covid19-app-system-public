package uk.nhs.nhsx.circuitbreakers

import java.security.SecureRandom
import java.util.*

object ApprovalTokenGenerator {

    private val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    operator fun invoke(random: Random = SecureRandom()) =
        (1..50)
            .map { random.nextInt(charPool.size) }
            .asSequence()
            .map(charPool::get)
            .joinToString("")
}
