package uk.nhs.nhsx.diagnosiskeydist.apispec;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DailyZIPSubmissionPeriodTest {

    @Test
    public void testZipPath() {
        Date endDate = utcDate(2020, 7, 20, 0, 0, 0, 0);
        DailyZIPSubmissionPeriod dailyZIPSubmissionPeriod = new DailyZIPSubmissionPeriod(endDate);
        assertEquals("distribution/daily/2020072000.zip", dailyZIPSubmissionPeriod.zipPath());
    }

    @Test
    public void testIsCoveringSubmissionDate() {
        assertFalse(new DailyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 2, 23, 59, 59, 999), 0));
        assertTrue(new DailyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 3, 0, 0, 0, 0), 0));
        assertTrue(new DailyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 3, 23, 59, 59, 999), 0));
        assertFalse(new DailyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 4, 0, 0, 0, 0), 0));

        assertTrue(new DailyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 2, 23, 59, 59, 999), -15));
        assertFalse(new DailyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 3, 23, 59, 59, 999), -15));
    }

    @Test
    public void testPeriodForSubmissionDate() {
        assertEquals("distribution/daily/2020070500.zip", DailyZIPSubmissionPeriod.periodForSubmissionDate(utcDate(2020, 07, 4, 0, 0, 0, 0)).zipPath());
        assertEquals("distribution/daily/2020070500.zip", DailyZIPSubmissionPeriod.periodForSubmissionDate(utcDate(2020, 07, 4, 23, 59, 59, 999)).zipPath());
    }

    @Test
    public void testAllPeriodsToGenerate() {
        Date endDate = utcDate(2020, 7, 20, 0, 0, 0, 0);
        DailyZIPSubmissionPeriod dailyZIPSubmissionPeriod = new DailyZIPSubmissionPeriod(endDate);

        List<DailyZIPSubmissionPeriod> result = dailyZIPSubmissionPeriod.allPeriodsToGenerate();
        assertEquals(15, result.size());

        //Special case: zip "unofficially" available during the day and contains the keys submitted so far (the mobile apps may only download the ZIP after the end of the day):
        assertEquals("distribution/daily/2020072000.zip", result.get(0).zipPath());

        //Regular cases (API spec)
        assertEquals("distribution/daily/2020071900.zip", result.get(1).zipPath());
        assertEquals("distribution/daily/2020070600.zip", result.get(14).zipPath());
    }

    private int getDayOfMonth(Date dateToConvert) {
        LocalDateTime dateTime = Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return dateTime.getDayOfMonth();
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
}
