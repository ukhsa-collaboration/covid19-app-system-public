package uk.nhs.nhsx.diagnosiskeydist.agspec

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class ENIntervalNumberTest {

    @Test
    fun testValidForDateEarlierThanKeyGeneration() {
        val keyGeneratedAt = utcDate(2020, 7, 7, 0, 0, 0, 0)
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(keyGeneratedAt)
        val beforeKeyGeneration = utcDate(2020, 7, 6, 0, 0, 0, 0)
        assertFalse(enIntervalNumber.validUntil(beforeKeyGeneration))
    }

    @Test
    fun testValidUntil() {
        val keyGeneratedAt = utcDate(2020, 7, 7, 0, 0, 0, 0)
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(keyGeneratedAt)
        val threeDaysAfterKeyGeneration = utcDate(2020, 7, 10, 0, 0, 0, 0)
        val twentyDaysAfterGeneration = utcDate(2020, 7, 27, 0, 0, 0, 0)
        assertTrue(enIntervalNumber.validUntil(threeDaysAfterKeyGeneration))
        assertFalse(enIntervalNumber.validUntil(twentyDaysAfterGeneration))
    }

    @Test
    fun testValidUntil2() {
        val dateNow = ENIntervalNumber.enIntervalNumberFromTimestamp(Date()).toTimestamp()
        val enIntervalNumberNow = ENIntervalNumber.enIntervalNumberFromTimestamp(dateNow)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = dateNow
        cal.add(Calendar.DATE, 15)
        val nowPlus15Days = cal.time
        cal.add(Calendar.HOUR_OF_DAY, -2)
        val nowPlus14DaysMinusTwoHours = cal.time
        assertFalse(enIntervalNumberNow.validUntil(nowPlus15Days))
        assertTrue(enIntervalNumberNow.validUntil(nowPlus14DaysMinusTwoHours))
    }

    @Test
    fun testEnIntervalNumberFromTimestamp() {
        assertEquals(2656614, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).enIntervalNumber)
    }

    @Test
    fun testEnIntervalNumberFromTimestampInMillis() {
        assertEquals(2656614, ENIntervalNumber.enIntervalNumberFromTimestampInMillis(utcDate(2020, 7, 5, 17, 0, 0, 0).time).enIntervalNumber)
    }

    @Test
    fun testEnIntervalNumberFromTimestampInUnixEpochTime() {
        assertEquals(2656614, ENIntervalNumber.enIntervalNumberFromTimestampInUnixEpochTime(utcDate(2020, 7, 5, 17, 0, 0, 0).time / 1000).enIntervalNumber)
    }

    @Test
    fun testToTimestampInUnixEpochTime() {
        assertEquals(1593968400L, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).toTimestampInUnixEpochTime())
    }

    @Test
    fun testToTimestampInMillis() {
        assertEquals(1593968400000L, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).toTimestampInMillis())
    }

    @Test
    fun testToTimestamp() {
        assertEquals(1593968400000L, ENIntervalNumber.enIntervalNumberFromTimestamp(utcDate(2020, 7, 5, 17, 0, 0, 0)).toTimestamp().time)
    }

    @Test
    fun testToString() {
        val keyGeneratedDate = utcDate(2020, 7, 5, 17, 0, 0, 0)
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(keyGeneratedDate)
        assertEquals("ENIntervalNumber(2656614: 2020-07-05 17:00)", enIntervalNumber.toString())
    }

    private fun utcDate(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, millis: Int): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal[Calendar.YEAR] = year
        cal[Calendar.MONTH] = month - 1
        cal[Calendar.DAY_OF_MONTH] = day
        cal[Calendar.HOUR_OF_DAY] = hour
        cal[Calendar.MINUTE] = minute
        cal[Calendar.SECOND] = second
        cal[Calendar.MILLISECOND] = millis
        return cal.time
    }
}