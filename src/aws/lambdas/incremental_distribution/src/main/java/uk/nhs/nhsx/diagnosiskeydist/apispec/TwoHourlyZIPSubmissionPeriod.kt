package uk.nhs.nhsx.diagnosiskeydist.apispec

import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField.HOUR_OF_DAY
import java.time.temporal.ChronoField.MINUTE_OF_HOUR
import java.time.temporal.ChronoField.NANO_OF_SECOND
import java.time.temporal.ChronoField.SECOND_OF_MINUTE
import java.time.temporal.ChronoUnit.HOURS
import java.util.*

data class TwoHourlyZIPSubmissionPeriod(val periodEndDateExclusive: Instant) : ZIPSubmissionPeriod {

    init {
        requireValid(periodEndDateExclusive)
    }

    override fun zipPath(): String = "$TWO_HOURLY_PATH_PREFIX${twoHourlyKey()}.zip"

    private fun twoHourlyKey(): String = HOURLY_FORMAT.format(periodEndDateExclusive)

    /**
     * @return true, if `diagnosisKeySubmissionDate` is covered by the two-hourly period represented by (`twoHourlyDate` shifted by `twoHourlyDateOffsetMinutes`)
     */
    override fun isCoveringSubmissionDate(diagnosisKeySubmission: Instant, periodOffset: Duration): Boolean {
        val toExclusive = periodEndDateExclusive.plus(periodOffset)
        val fromInclusive = toExclusive.minus(Duration.ofHours(2))
        return (diagnosisKeySubmission.isAfter(fromInclusive) || diagnosisKeySubmission == fromInclusive)
            && diagnosisKeySubmission.isBefore(toExclusive)
    }

    /**
     * @return list of valid `TwoHourlyPeriod` ending with `this` `TwoHourlyPeriod`
     */
    override fun allPeriodsToGenerate(): List<TwoHourlyZIPSubmissionPeriod> {
        val periods = ArrayList<TwoHourlyZIPSubmissionPeriod>()
        var currentInstant = periodEndDateExclusive
        for (i in 0 until TOTAL_TWO_HOURLY_ZIPS) {
            periods.add(TwoHourlyZIPSubmissionPeriod(currentInstant))
            currentInstant = currentInstant.minus(Duration.ofHours(2))
        }
        return periods
    }

    override val startInclusive: Instant
        get() = periodEndDateExclusive.minus(Duration.ofHours(2))

    override val endExclusive: Instant
        get() = periodEndDateExclusive

    override fun toString(): String =
        "2 hours: from ${HOURLY_FORMAT.format(startInclusive)} (inclusive) to ${HOURLY_FORMAT.format(endExclusive)} (exclusive)"

    companion object {
        const val TWO_HOURLY_PATH_PREFIX = "distribution/two-hourly/"
        private const val TOTAL_TWO_HOURLY_ZIPS = ENIntervalNumber.MAX_DIAGNOSIS_KEY_AGE_DAYS * 12
        private val HOURLY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(UTC)

        private fun requireValid(endDate: Instant): Instant {
            endDate.atZone(UTC).also {
                check(it[HOUR_OF_DAY] % 2 == 0)
                check(it[MINUTE_OF_HOUR] == 0)
                check(it[SECOND_OF_MINUTE] == 0)
                check(it[NANO_OF_SECOND] == 0)
            }
            return endDate
        }

        /**
         * @return end date (exclusive) of the two-hourly period comprising the Diagnosis Keys posted to the Submission Service at `diagnosisKeySubmission`
         */
        fun periodForSubmissionDate(diagnosisKeySubmission: Instant): TwoHourlyZIPSubmissionPeriod {
            val hour = diagnosisKeySubmission
                .truncatedTo(HOURS)
                .atZone(UTC)
                .hour
            return TwoHourlyZIPSubmissionPeriod(
                diagnosisKeySubmission
                    .truncatedTo(HOURS)
                    .atZone(UTC)
                    .withHour(hour - hour % 2)
                    .plus(Duration.ofHours(2))
                    .toInstant()
            )
        }
    }
}
