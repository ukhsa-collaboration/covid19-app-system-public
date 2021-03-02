package uk.nhs.nhsx.circuitbreakerstats;

import java.time.*;

class ServiceWindow {
    private final LocalDate yesterday;
    private final LocalDate today;


    ServiceWindow(Instant time) {
        today = time.atZone(ZoneOffset.UTC).toLocalDate();
        yesterday = today.minusDays(1);
    }

    long queryStart() {
        return ZonedDateTime.of(yesterday, LocalTime.of(0, 0, 0), ZoneOffset.UTC).toEpochSecond();

    }

    long queryEnd() {
        return ZonedDateTime.of(yesterday, LocalTime.of(23, 59, 59), ZoneOffset.UTC).toEpochSecond();

    }
}
