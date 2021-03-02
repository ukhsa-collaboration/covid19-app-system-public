package uk.nhs.nhsx.diagnosiskeydist.apispec;

import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.HOURS;
import static uk.nhs.nhsx.core.Preconditions.checkState;

public class TwoHourlyZIPSubmissionPeriod implements ZIPSubmissionPeriod {
    private static final String TWO_HOURLY_PATH_PREFIX = "distribution/two-hourly/";
    private static final int TOTAL_TWO_HOURLY_ZIPS = ENIntervalNumber.MAX_DIAGNOSIS_KEY_AGE_DAYS * 12;
    private static final DateTimeFormatter HOURLY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(UTC);

    private final Instant periodEndDateExclusive;

    public TwoHourlyZIPSubmissionPeriod(Instant dailyPeriodEndDate) {
        periodEndDateExclusive = requireValid(dailyPeriodEndDate);
    }

    private static Instant requireValid(Instant endDate) {
        var date = endDate.atZone(UTC);
        checkState(date.get(HOUR_OF_DAY) % 2 == 0);
        checkState(date.get(MINUTE_OF_HOUR) == 0);
        checkState(date.get(SECOND_OF_MINUTE) == 0);
        checkState(date.get(NANO_OF_SECOND) == 0);
        return endDate;
    }

    /**
     * @return end date (exclusive) of the two-hourly period comprising the Diagnosis Keys posted to the Submission Service at <code>diagnosisKeySubmission</code>
     */
    public static TwoHourlyZIPSubmissionPeriod periodForSubmissionDate(Instant diagnosisKeySubmission) {
        var hour = diagnosisKeySubmission
            .truncatedTo(HOURS)
            .atZone(UTC)
            .getHour();

        return new TwoHourlyZIPSubmissionPeriod(
            diagnosisKeySubmission
                .truncatedTo(HOURS)
                .atZone(UTC)
                .withHour(hour - (hour % 2))
                .plus(Duration.ofHours(2))
                .toInstant()
        );
    }

    @Override
    public String zipPath() {
        return TWO_HOURLY_PATH_PREFIX + twoHourlyKey() + ".zip";
    }

    private String twoHourlyKey() {
        return HOURLY_FORMAT.format(periodEndDateExclusive);
    }

    /**
     * @return true, if <code>diagnosisKeySubmissionDate</code> is covered by the two-hourly period represented by (<code>twoHourlyDate</code> shifted by <code>twoHourlyDateOffsetMinutes</code>)
     */
    @Override
    public boolean isCoveringSubmissionDate(Instant diagnosisKeySubmission, Duration offset) {
        var toExclusive = periodEndDateExclusive
            .plus(offset);

        var fromInclusive = toExclusive
            .minus(Duration.ofHours(2));

        return (diagnosisKeySubmission.isAfter(fromInclusive) || diagnosisKeySubmission.equals(fromInclusive)) && diagnosisKeySubmission.isBefore(toExclusive);
    }

    /**
     * @return list of valid <code>TwoHourlyPeriod</code> ending with <code>this</code> <code>TwoHourlyPeriod</code>
     */
    @Override
    public List<TwoHourlyZIPSubmissionPeriod> allPeriodsToGenerate() {
        var periods = new ArrayList<TwoHourlyZIPSubmissionPeriod>();
        var currentInstant = periodEndDateExclusive;
        for (var i = 0; i < TOTAL_TWO_HOURLY_ZIPS; i++) {
            periods.add(new TwoHourlyZIPSubmissionPeriod(currentInstant));
            currentInstant = currentInstant.minus(Duration.ofHours(2));
        }
        return periods;
    }

    @Override
    public Instant getStartInclusive() {
        return periodEndDateExclusive.minus(Duration.ofHours(2));
    }

    @Override
    public Instant getEndExclusive() {
        return periodEndDateExclusive;
    }

    @Override
    public String toString() {
        return format("2 hours: from %s (inclusive) to %s (exclusive)",
            HOURLY_FORMAT.format(getStartInclusive()),
            HOURLY_FORMAT.format(getEndExclusive()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TwoHourlyZIPSubmissionPeriod that = (TwoHourlyZIPSubmissionPeriod) o;
        return Objects.equals(periodEndDateExclusive, that.periodEndDateExclusive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodEndDateExclusive);
    }
}
