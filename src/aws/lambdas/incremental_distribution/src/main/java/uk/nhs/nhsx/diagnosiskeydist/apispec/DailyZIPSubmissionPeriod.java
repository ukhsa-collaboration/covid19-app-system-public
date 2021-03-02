package uk.nhs.nhsx.diagnosiskeydist.apispec;

import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.DAYS;
import static uk.nhs.nhsx.core.Preconditions.checkState;

public class DailyZIPSubmissionPeriod implements ZIPSubmissionPeriod {
    private static final String DAILY_PATH_PREFIX = "distribution/daily/";
    private static final int TOTAL_DAILY_ZIPS = ENIntervalNumber.MAX_DIAGNOSIS_KEY_AGE_DAYS;
    private static final DateTimeFormatter HOURLY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'00'").withZone(UTC);

    private final Instant periodEndDateExclusive;

    public DailyZIPSubmissionPeriod(Instant periodEndDateExclusive) {
        this.periodEndDateExclusive = requireValid(periodEndDateExclusive);
    }

    private static Instant requireValid(Instant endDate) {
        var date = endDate.atZone(UTC);
        checkState(date.get(HOUR_OF_DAY) == 0);
        checkState(date.get(MINUTE_OF_HOUR) == 0);
        checkState(date.get(SECOND_OF_MINUTE) == 0);
        checkState(date.get(NANO_OF_SECOND) == 0);
        return endDate;
    }

    public static DailyZIPSubmissionPeriod periodForSubmissionDate(Instant diagnosisKeySubmission) {
        return new DailyZIPSubmissionPeriod(
            diagnosisKeySubmission
                .truncatedTo(DAYS)
                .plus(Duration.ofDays(1)));
    }

    @Override
    public String zipPath() {
        return DAILY_PATH_PREFIX + dailyKey() + ".zip";
    }

    private String dailyKey() {
        return HOURLY_FORMAT.format(periodEndDateExclusive);
    }

    @Override
    public boolean isCoveringSubmissionDate(Instant diagnosisKeySubmission, Duration offset) {
        var toExclusive = periodEndDateExclusive
            .plus(offset);

        var fromInclusive = toExclusive
            .minus(Duration.ofDays(1));

        return (diagnosisKeySubmission.isAfter(fromInclusive) || diagnosisKeySubmission.equals(fromInclusive)) && diagnosisKeySubmission.isBefore(toExclusive);
    }

    /**
     * returns DailyPeriods for the past 14 days
     */
    @Override
    public List<DailyZIPSubmissionPeriod> allPeriodsToGenerate() {
        var periods = new ArrayList<DailyZIPSubmissionPeriod>();
        var currentInstant = periodEndDateExclusive;
        for (var i = 0; i < TOTAL_DAILY_ZIPS + 1; i++) {
            periods.add(new DailyZIPSubmissionPeriod(currentInstant));
            currentInstant = currentInstant.minus(Duration.ofDays(1));
        }
        return periods;
    }

    @Override
    public Instant getStartInclusive() {
        return periodEndDateExclusive.minus(Duration.ofDays(1));
    }

    @Override
    public Instant getEndExclusive() {
        return periodEndDateExclusive;
    }

    @Override
    public String toString() {
        return format("1 day: from %s (inclusive) to %s (exclusive)",
            HOURLY_FORMAT.format(getStartInclusive()),
            HOURLY_FORMAT.format(getEndExclusive()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyZIPSubmissionPeriod that = (DailyZIPSubmissionPeriod) o;
        return Objects.equals(periodEndDateExclusive, that.periodEndDateExclusive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodEndDateExclusive);
    }
}
