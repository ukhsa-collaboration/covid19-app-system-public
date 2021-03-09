package uk.nhs.nhsx.diagnosiskeydist.apispec

import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit.DAYS
import java.util.*

class DailyZIPSubmissionPeriod(periodEndDateExclusive: Instant) : ZIPSubmissionPeriod {
    private val periodEndDateExclusive: Instant = requireValid(periodEndDateExclusive)

    override fun zipPath(): String = "$DAILY_PATH_PREFIX${dailyKey()}.zip"

    private fun dailyKey(): String = HOURLY_FORMAT.format(periodEndDateExclusive)

    override fun isCoveringSubmissionDate(diagnosisKeySubmission: Instant, periodOffset: Duration): Boolean {
        val toExclusive = periodEndDateExclusive.plus(periodOffset)
        val fromInclusive = toExclusive.minus(Duration.ofDays(1))
        return (diagnosisKeySubmission.isAfter(fromInclusive) || diagnosisKeySubmission == fromInclusive) && diagnosisKeySubmission.isBefore(toExclusive)
    }

    /**
     * returns DailyPeriods for the past 14 days
     */
    override fun allPeriodsToGenerate(): List<DailyZIPSubmissionPeriod> {
        val periods = ArrayList<DailyZIPSubmissionPeriod>()
        var currentInstant = periodEndDateExclusive
        for (i in 0 until TOTAL_DAILY_ZIPS + 1) {
            periods.add(DailyZIPSubmissionPeriod(currentInstant))
            currentInstant = currentInstant.minus(Duration.ofDays(1))
        }
        return periods
    }

    override val startInclusive: Instant
        get() = periodEndDateExclusive.minus(Duration.ofDays(1))

    override val endExclusive: Instant
        get() = periodEndDateExclusive

    override fun toString(): String =
        "1 day: from ${HOURLY_FORMAT.format(startInclusive)} (inclusive) to ${HOURLY_FORMAT.format(endExclusive)} (exclusive)"

    companion object {
        private const val DAILY_PATH_PREFIX = "distribution/daily/"
        private const val TOTAL_DAILY_ZIPS = ENIntervalNumber.MAX_DIAGNOSIS_KEY_AGE_DAYS
        private val HOURLY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'00'").withZone(UTC)

        private fun requireValid(endDate: Instant): Instant {
            endDate.atZone(UTC).also {
                check(it[ChronoField.HOUR_OF_DAY] == 0)
                check(it[ChronoField.MINUTE_OF_HOUR] == 0)
                check(it[ChronoField.SECOND_OF_MINUTE] == 0)
                check(it[ChronoField.NANO_OF_SECOND] == 0)
            }
            return endDate
        }

        @JvmStatic
        fun periodForSubmissionDate(diagnosisKeySubmission: Instant): DailyZIPSubmissionPeriod {
            return DailyZIPSubmissionPeriod(
                diagnosisKeySubmission
                    .truncatedTo(DAYS)
                    .plus(Duration.ofDays(1)))
        }
    }
}
