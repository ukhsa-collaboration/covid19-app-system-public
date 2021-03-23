package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.core.Clock
import java.time.Duration

class VirologyDataTimeToLiveCalculator(
    private val testDataExpiryDuration: Duration, private val submissionDataExpiryDuration: Duration
) : (Clock) -> VirologyDataTimeToLive {

    override fun invoke(clock: Clock): VirologyDataTimeToLive {
        val now = clock()
        return VirologyDataTimeToLive(now + testDataExpiryDuration, now + submissionDataExpiryDuration)
    }

    companion object {
        val DEFAULT_TTL = VirologyDataTimeToLiveCalculator(Duration.ofHours(4), Duration.ofDays(4))
    }
}
