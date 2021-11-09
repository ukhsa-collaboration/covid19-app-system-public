package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.core.Clock
import java.time.Instant
import java.time.Period

object TestOrderExpiryCalculator : (Clock) -> Instant {
    val DEFAULT_EXPIRY: Period = Period.ofWeeks(4)
    override fun invoke(clock: Clock): Instant = clock().plus(DEFAULT_EXPIRY)
}
