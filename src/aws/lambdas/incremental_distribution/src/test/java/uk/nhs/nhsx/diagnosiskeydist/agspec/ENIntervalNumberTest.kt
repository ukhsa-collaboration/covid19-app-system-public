@file:Suppress("UsePropertyAccessSyntax")

package uk.nhs.nhsx.diagnosiskeydist.agspec

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

class ENIntervalNumberTest {

    @ParameterizedTest
    @CsvSource(
        "2020-07-07T00:00:00.000Z,2020-07-06T00:00:00.000Z,false",
        "2020-07-07T00:00:00.000Z,2020-07-27T00:00:00.000Z,false",
        "2020-07-07T00:00:00.000Z,2020-07-22T00:00:00.000Z,false",
        "2020-07-07T00:00:00.000Z,2020-07-10T00:00:00.000Z,true",
        "2020-07-07T00:00:00.000Z,2020-07-21T22:00:00.000Z,true"
    )
    fun `valid until`(now: String, validUntil: String, expected: Boolean) {
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(now.asInstant())
        assertThat(enIntervalNumber.validUntil(validUntil.asInstant())).isEqualTo(expected)
    }

    @Test
    fun `create enIntervalNumber from timestamp`() {
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp("2020-07-05T17:00:00.000Z".asInstant())
        assertThat(enIntervalNumber.enIntervalNumber).isEqualTo(2656614L)
    }

    @Test
    fun `create enIntervalNumber from timestamp millis`() {
        val now = "2020-07-05T17:00:00.000Z".asInstant()
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestampInMillis(now.toEpochMilli())
        assertThat(enIntervalNumber.enIntervalNumber).isEqualTo(2656614L)
    }

    @Test
    fun `create enIntervalNumber from timestamp unix epoch time`() {
        val now = "2020-07-05T17:00:00.000Z".asInstant()
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestampInUnixEpochTime(now.epochSecond)
        assertThat(enIntervalNumber.enIntervalNumber).isEqualTo(2656614L)
    }

    @Test
    fun `as TimestampInUnixEpochTime`() {
        val now = "2020-07-05T17:00:00.000Z".asInstant()
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(now)
        assertThat(enIntervalNumber.toTimestampInUnixEpochTime()).isEqualTo(1593968400L)
    }

    @Test
    fun `as TimestampInMillis`() {
        val now = "2020-07-05T17:00:00.000Z".asInstant()
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(now)
        assertThat(enIntervalNumber.toTimestampInMillis()).isEqualTo(1593968400000L)
    }

    @Test
    fun `as Timestamp`() {
        val now = "2020-07-05T17:00:00.000Z".asInstant()
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(now)
        assertThat(enIntervalNumber.toTimestamp().toEpochMilli()).isEqualTo(1593968400000L)
    }

    @Test
    fun `as String`() {
        val now = "2020-07-05T17:00:00.000Z".asInstant()
        val enIntervalNumber = ENIntervalNumber.enIntervalNumberFromTimestamp(now)
        assertThat(enIntervalNumber.toString()).isEqualTo("ENIntervalNumber(2656614: 2020-07-05 17:00)")
    }

    private fun String.asInstant() = Instant.parse(this)
}
