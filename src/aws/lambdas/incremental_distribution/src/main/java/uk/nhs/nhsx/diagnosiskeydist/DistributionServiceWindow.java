package uk.nhs.nhsx.diagnosiskeydist;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class DistributionServiceWindow {

    public static final Duration ZIP_SUBMISSION_PERIOD_OFFSET = Duration.ofMinutes(-15);
    private static final Duration DISTRIBUTION_EARLIEST_START_OFFSET = ZIP_SUBMISSION_PERIOD_OFFSET.plusMinutes(1);
    private static final Duration DISTRIBUTION_LATEST_START_OFFSET = ZIP_SUBMISSION_PERIOD_OFFSET.plusMinutes(3);
    private static final Duration DISTRIBUTION_FREQUENCY = Duration.ofHours(2);

    private final Instant now;

    public DistributionServiceWindow(Instant now) {
        this.now = now;
    }

    public Instant nextWindow() {
        int hour = now.atZone(ZoneOffset.UTC).getHour();

        return now
            .truncatedTo(ChronoUnit.HOURS)
            .atZone(ZoneOffset.UTC)
            .withHour(hour - (hour % 2))
            .plus(Duration.ofHours(2))
            .toInstant();
    }

    public Instant zipExpirationExclusive() {
        return nextWindow().plus(DISTRIBUTION_FREQUENCY);
    }

    public Instant earliestBatchStartDateWithinHourInclusive() {
        return nextWindow().plus(DISTRIBUTION_EARLIEST_START_OFFSET);
    }

    public Instant latestBatchStartDateWithinHourExclusive() {
        return nextWindow().plus(DISTRIBUTION_LATEST_START_OFFSET);
    }

    public boolean isValidBatchStartDate() {
        return (!now.isBefore(earliestBatchStartDateWithinHourInclusive())) && now.isBefore(latestBatchStartDateWithinHourExclusive());
    }
}
