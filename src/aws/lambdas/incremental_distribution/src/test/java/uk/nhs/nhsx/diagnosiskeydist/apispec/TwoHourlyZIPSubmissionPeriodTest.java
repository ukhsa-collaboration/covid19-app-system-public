package uk.nhs.nhsx.diagnosiskeydist.apispec;

import org.junit.Test;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TwoHourlyZIPSubmissionPeriodTest {

    private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

    @Test
    public void testZipPath() {
        Date endDate = utcDate(2020, 7, 20, 16, 0, 0, 0);
        TwoHourlyZIPSubmissionPeriod dailyZIPSubmissionPeriod = new TwoHourlyZIPSubmissionPeriod(endDate);
        assertEquals("distribution/two-hourly/2020072016.zip", dailyZIPSubmissionPeriod.zipPath());
    }

    @Test
    public void testIsCoveringSubmissionDate() {
        assertFalse(new TwoHourlyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 3, 21, 59, 59, 999), 0));
        assertTrue(new TwoHourlyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 3, 22, 0, 0, 0), 0));
        assertTrue(new TwoHourlyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 3, 23, 59, 59, 999), 0));
        assertFalse(new TwoHourlyZIPSubmissionPeriod(utcDate(2020, 07, 4, 0, 0, 0, 0))
                .isCoveringSubmissionDate(utcDate(2020, 07, 4, 0, 0, 0, 0), 0));
    }

    @Test
    public void testTwoHourlyDate() {
        Date almostMidnight = utcDate(2020, 07, 3, 23, 59, 59, 999);
        TwoHourlyZIPSubmissionPeriod midnight = TwoHourlyZIPSubmissionPeriod.periodForSubmissionDate(almostMidnight);

        Calendar cal = utcCalendar(almostMidnight);
        cal.add(Calendar.MILLISECOND, 1);
        Date expectedDate = cal.getTime();

        assertEquals(expectedDate, midnight.getEndExclusive());

        Date afterMidnight = utcDate(2020, 07, 3, 0, 1, 0, 0);
        TwoHourlyZIPSubmissionPeriod twoInTheMorning = TwoHourlyZIPSubmissionPeriod.periodForSubmissionDate(afterMidnight);

        cal = utcCalendar(afterMidnight);
        cal.add(Calendar.MINUTE, 59);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        expectedDate = cal.getTime();

        assertEquals(expectedDate, twoInTheMorning.getEndExclusive());
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

    private static Calendar utcCalendar(Date dailyDate) {
        Calendar cal = Calendar.getInstance(TIME_ZONE_UTC);
        cal.setTime(dailyDate);

        return cal;
    }
}
