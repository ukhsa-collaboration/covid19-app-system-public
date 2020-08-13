package uk.nhs.nhsx.diagnosiskeydist.agspec;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class ENIntervalNumberTest {

    @Test
    public void testValidForDateEarlierThanKeyGeneration() {
        Date keyGeneratedAt = utcDate(2020, 7, 7, 0, 0, 0, 0);
        ENIntervalNumber enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(keyGeneratedAt);

        Date beforeKeyGeneration = utcDate(2020, 7, 6, 0,0,0, 0);
        assertFalse(enIntervalNumber.validUntil(beforeKeyGeneration));
    }

    @Test
    public void testValidUntil() {
        Date keyGeneratedAt = utcDate(2020, 7, 7, 0, 0, 0, 0);
        ENIntervalNumber enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(keyGeneratedAt);

        Date threeDaysAfterKeyGeneration = utcDate(2020, 7, 10, 0,0,0, 0);
        Date twentyDaysAfterGeneration = utcDate(2020, 7, 27, 0,0,0, 0);

        assertTrue(enIntervalNumber.validUntil(threeDaysAfterKeyGeneration));
        assertFalse(enIntervalNumber.validUntil(twentyDaysAfterGeneration));
    }

    @Test
    public void testValidUntil2() {
        Date dateNow = ENIntervalNumber.enIntervalNumberFromTimestamp(new Date()).toTimestamp();
        ENIntervalNumber enIntervalNumberNow = ENIntervalNumber.enIntervalNumberFromTimestamp(dateNow);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(dateNow);
        cal.add(Calendar.DATE, 15);
        Date nowPlus15Days = cal.getTime();

        cal.add(Calendar.HOUR_OF_DAY, -2);
        Date nowPlus14DaysMinusTwoHours = cal.getTime();

        assertFalse(enIntervalNumberNow.validUntil(nowPlus15Days));
        assertTrue(enIntervalNumberNow.validUntil(nowPlus14DaysMinusTwoHours));
    }

    @Test
    public void testEnIntervalNumberFromTimestamp() {
        assertEquals(2656614, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).getEnIntervalNumber());
    }

    @Test
    public void testEnIntervalNumberFromTimestampInMillis() {
        assertEquals(2656614, ENIntervalNumber.enIntervalNumberFromTimestampInMillis(utcDate(2020, 7, 5, 17, 0, 0, 0).getTime()).getEnIntervalNumber());
    }

    @Test
    public void testEnIntervalNumberFromTimestampInUnixEpochTime() {
        assertEquals(2656614, ENIntervalNumber.enIntervalNumberFromTimestampInUnixEpochTime(utcDate(2020, 7, 5, 17, 0, 0, 0).getTime() / 1000).getEnIntervalNumber());
    }

    @Test
    public void testToTimestampInUnixEpochTime() {
        assertEquals(1593968400l, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).toTimestampInUnixEpochTime());
    }

    @Test
    public void testToTimestampInMillis() {
        assertEquals(1593968400000l, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).toTimestampInMillis());
    }

    @Test
    public void testToTimestamp() {
        assertEquals(1593968400000l, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).toTimestamp().getTime());
    }

    @Test
    public void testToString() {
        Date keyGeneratedDate = utcDate(2020, 7, 5, 17, 0, 0, 0);
        ENIntervalNumber enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(keyGeneratedDate);

        assertEquals("ENIntervalNumber(2656614: 2020-07-05 17:00)", enIntervalNumber.toString());
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
