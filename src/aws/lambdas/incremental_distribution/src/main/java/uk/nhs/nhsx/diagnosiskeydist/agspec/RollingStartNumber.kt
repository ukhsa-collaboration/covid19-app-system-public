package uk.nhs.nhsx.diagnosiskeydist.agspec

import uk.nhs.nhsx.core.Clock
import java.time.Duration

object RollingStartNumber {
    fun isRollingStartNumberValid(
        clock: Clock,
        rollingStartNumber: Long,
        rollingPeriod: Int
    ): Boolean {
        val now = clock()
        val currentInstant = ENIntervalNumber.enIntervalNumberFromTimestamp(now).enIntervalNumber
        val expiryPeriod = ENIntervalNumber.enIntervalNumberFromTimestamp(now.minus(Duration.ofDays(14))).enIntervalNumber
        return rollingStartNumber + rollingPeriod >= expiryPeriod && rollingStartNumber <= currentInstant
    }
}
