package uk.nhs.nhsx.virology.persistence;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class VirologyDataTimeToLiveCalculator implements Function<Supplier<Instant>, VirologyDataTimeToLive> {

    public static VirologyDataTimeToLiveCalculator DEFAULT_TTL = new VirologyDataTimeToLiveCalculator(Duration.ofHours(4), Duration.ofDays(4));
    public static VirologyDataTimeToLiveCalculator CTA_EXCHANGE_TTL = new VirologyDataTimeToLiveCalculator(Duration.ofMinutes(10), Duration.ofMinutes(30));

    private final Duration testDataExpiryDuration;
    private final Duration submissionDataExpiryDuration;

    VirologyDataTimeToLiveCalculator(Duration testDataExpiryDuration, Duration submissionDataExpiryDuration) {
        this.testDataExpiryDuration = testDataExpiryDuration;
        this.submissionDataExpiryDuration = submissionDataExpiryDuration;
    }

    @Override
    public VirologyDataTimeToLive apply(Supplier<Instant> clock) {
        Instant instant = clock.get();
        long testDataExpireAt = instant.plus(testDataExpiryDuration.getSeconds(), ChronoUnit.SECONDS).getEpochSecond();
        long submissionDataExpireAt = instant.plus(submissionDataExpiryDuration.getSeconds(), ChronoUnit.SECONDS).getEpochSecond();
        return new VirologyDataTimeToLive(
            testDataExpireAt,
            submissionDataExpireAt
        );
    }
}
