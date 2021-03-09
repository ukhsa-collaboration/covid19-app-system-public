package uk.nhs.nhsx.diagnosiskeydist

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.HOURS

class DistributionServiceWindow(private val now: Instant, val zipSubmissionPeriodOffset: Duration) {
    private val distributionEarliestStartOffset = zipSubmissionPeriodOffset.plusMinutes(1)
    private val distributionLatestStartOffset = zipSubmissionPeriodOffset.plusMinutes(3)
    private val distributionFrequency = Duration.ofHours(2)

    fun nextWindow(): Instant = now.atZone(UTC).hour
        .let {
            now
                .truncatedTo(HOURS)
                .atZone(UTC)
                .withHour(it - (it % 2))
                .plus(Duration.ofHours(2))
                .toInstant()

        }

    fun zipExpirationExclusive(): Instant = nextWindow().plus(distributionFrequency)

    fun earliestBatchStartDateWithinHourInclusive(): Instant = nextWindow().plus(distributionEarliestStartOffset)

    fun latestBatchStartDateWithinHourExclusive(): Instant = nextWindow().plus(distributionLatestStartOffset)

    fun isValidBatchStartDate(): Boolean = !now.isBefore(earliestBatchStartDateWithinHourInclusive())
        && now.isBefore(latestBatchStartDateWithinHourExclusive())
}
