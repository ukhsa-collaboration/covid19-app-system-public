package uk.nhs.nhsx.virology.persistence

import java.time.Duration
import java.time.Instant
import java.util.function.Supplier

class VirologyDataTimeToLiveCalculator(
    private val testDataExpiryDuration: Duration, private val submissionDataExpiryDuration: Duration
) : (Supplier<Instant>) -> VirologyDataTimeToLive {

    override fun invoke(clock: Supplier<Instant>): VirologyDataTimeToLive {
        val now = clock.get()
        return VirologyDataTimeToLive(now + testDataExpiryDuration, now + submissionDataExpiryDuration)
    }

    companion object {
        val DEFAULT_TTL = VirologyDataTimeToLiveCalculator(Duration.ofHours(4), Duration.ofDays(4))
    }
}
