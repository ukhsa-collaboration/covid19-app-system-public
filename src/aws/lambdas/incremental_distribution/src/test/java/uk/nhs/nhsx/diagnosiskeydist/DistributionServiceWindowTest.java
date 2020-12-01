package uk.nhs.nhsx.diagnosiskeydist;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DistributionServiceWindowTest {
    @Test
    public void testIsDistributionTimeWindowStartInvalid() {
        DistributionServiceWindow earlyStartDate = new DistributionServiceWindow(utcDate(2020, 7, 4, 11, 45, 59, 999));
        DistributionServiceWindow lateStartDate = new DistributionServiceWindow(utcDate(2020, 7, 4, 11, 48, 0, 0));
        DistributionServiceWindow evenHourEarlyStartDate = new DistributionServiceWindow(utcDate(2020, 7, 4, 12, 46, 0, 0));
        DistributionServiceWindow evenHourLateStartDate = new DistributionServiceWindow(utcDate(2020, 7, 4, 12, 47, 0, 0));
        assertFalse(earlyStartDate.validBatchStartDate());
        assertFalse(lateStartDate.validBatchStartDate());
        assertFalse(evenHourEarlyStartDate.validBatchStartDate());
        assertFalse(evenHourLateStartDate.validBatchStartDate());
    }

    @Test
    public void testIsDistributionTimeWindowStartValid() {
    	DistributionServiceWindow earlyStartDate = new DistributionServiceWindow(utcDate(2020, 7, 4, 11, 46, 0, 0));
        DistributionServiceWindow lateStartDate = new DistributionServiceWindow(utcDate(2020, 7, 4, 11, 47, 59, 999));
        assertTrue(earlyStartDate.validBatchStartDate());
        assertTrue(lateStartDate.validBatchStartDate());
    }

    private Date utcDate(int year, int month, int day, int hour, int minute, int second, int millis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millis);

        return cal.getTime();
    }

    @Test
    public void nextFullHour() {
        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 0, 0, 0, 0)
                ).nextFullHour().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 2, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 0, 0, 0)
                ).nextFullHour().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 2, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 59, 59, 999)
                ).nextFullHour().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 2, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 22, 0, 0, 0)
                ).nextFullHour().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 5, 0, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 0, 0, 0)
                ).nextFullHour().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 5, 0, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 59, 59, 999)
                ).nextFullHour().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 5, 0, 0, 0, 0));
    }

    @Test
    public void zipExpirationExclusive() {
        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 0, 0, 0, 0)
                ).zipExpirationExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 4, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 0, 0, 0)
                ).zipExpirationExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 4, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 59, 59, 999)
                ).zipExpirationExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 4, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 22, 0, 0, 0)
                ).zipExpirationExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 5, 2, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 0, 0, 0)
                ).zipExpirationExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 5, 2, 0, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 59, 59, 999)
                ).zipExpirationExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 5, 2, 0, 0, 0));
    }

    @Test
    public void earliestBatchStartDateWithinHourInclusive() {
        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 0, 0, 0, 0)
                ).earliestBatchStartDateWithinHourInclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 1, 46, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 0, 0, 0)
                ).earliestBatchStartDateWithinHourInclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 1, 46, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 59, 59, 999)
                ).earliestBatchStartDateWithinHourInclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 1, 46, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 22, 0, 0, 0)
                ).earliestBatchStartDateWithinHourInclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 23, 46, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 0, 0, 0)
                ).earliestBatchStartDateWithinHourInclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 23, 46, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 59, 59, 999)
                ).earliestBatchStartDateWithinHourInclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 23, 46, 0, 0));
    }

    @Test
    public void latestBatchStartDateWithinHourExclusive() {
        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 0, 0, 0, 0)
                ).latestBatchStartDateWithinHourExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 1, 48, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 0, 0, 0)
                ).latestBatchStartDateWithinHourExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 1, 48, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 1, 59, 59, 999)
                ).latestBatchStartDateWithinHourExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 1, 48, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 22, 0, 0, 0)
                ).latestBatchStartDateWithinHourExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 23, 48, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 0, 0, 0)
                ).latestBatchStartDateWithinHourExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 23, 48, 0, 0));

        assertThat(
                new DistributionServiceWindow(
                        utcDate(2020, 7, 4, 23, 59, 59, 999)
                ).latestBatchStartDateWithinHourExclusive().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        ).isEqualTo(LocalDateTime.of(2020, 7, 4, 23, 48, 0, 0));
    }
}
