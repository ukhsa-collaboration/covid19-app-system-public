package uk.nhs.nhsx.diagnosiskeydist.apispec

import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit.DAYS
import java.util.*

data class DailyZIPSubmissionPeriod(val periodEndDateExclusive: Instant) : ZIPSubmissionPeriod {

    init {
        requireValid(periodEndDateExclusive)
    }

    override fun zipPath(): String = zipPathFor(periodEndDateExclusive)

    override fun isCoveringSubmissionDate(diagnosisKeySubmission: Instant, periodOffset: Duration): Boolean {
        val toExclusive = periodEndDateExclusive.plus(periodOffset)
        val fromInclusive = toExclusive.minus(Duration.ofDays(1))
        return (diagnosisKeySubmission.isAfter(fromInclusive) || diagnosisKeySubmission == fromInclusive)
            && diagnosisKeySubmission.isBefore(toExclusive)
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
        const val DAILY_PATH_PREFIX = "distribution/daily/"
        private const val TOTAL_DAILY_ZIPS = ENIntervalNumber.MAX_DIAGNOSIS_KEY_AGE_DAYS
        private val HOURLY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'00'").withZone(UTC)
        private val HOURLY_FORMAT_IN = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(UTC)

        fun parseOrNull(key: String) = try {
            key
                .substringAfter(DAILY_PATH_PREFIX)
                .removeSuffix("00.zip")
                .let { HOURLY_FORMAT_IN.parse(it, LocalDate::from) }
                .atStartOfDay()
                .toInstant(UTC)
        } catch (e: Exception) {
            null
        }

        fun zipPathFor(instant: Instant) = "$DAILY_PATH_PREFIX${dailyKey(instant)}.zip"

        private fun dailyKey(instant: Instant): String = HOURLY_FORMAT.format(instant)

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
                    .plus(Duration.ofDays(1))
            )
        }
    }
}
