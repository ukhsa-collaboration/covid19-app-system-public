package uk.nhs.nhsx.diagnosiskeydist.apispec

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import uk.nhs.nhsx.testhelper.data.asInstant
import java.time.Duration

class DailyZIPSubmissionPeriodTest {

    @ParameterizedTest
    @CsvSource(
        "2020-07-20T00:00:00.000Z,false",
        "2020-07-20T05:00:00.000Z,true",
        "2020-07-20T00:05:00.000Z,true",
        "2020-07-20T00:00:05.000Z,true",
        "2020-07-20T00:00:00.005Z,true",
        "2020-07-20T05:05:05.005Z,true"
    )
    fun `throws exception if submission period is invalid`(periodEndDate: String, throwsEx: Boolean) {
        val endDate = periodEndDate.asInstant()

        if (throwsEx) {
            assertThatThrownBy { DailyZIPSubmissionPeriod(endDate) }.isInstanceOf(IllegalStateException::class.java)
        } else {
            assertThat(DailyZIPSubmissionPeriod(endDate)).isNotNull
        }
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-20T00:00:00.000Z,distribution/daily/2020072000.zip"
    )
    fun `creates ZIP path`(periodEndDate: String, expected: String) {
        DailyZIPSubmissionPeriod(periodEndDate.asInstant()).run {
            assertThat(zipPath()).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-04T00:00:00.000Z,2020-07-02T23:59:59.999Z,0,false",
        "2020-07-04T00:00:00.000Z,2020-07-03T00:00:00.000Z,0,true",
        "2020-07-04T00:00:00.000Z,2020-07-03T23:59:59.999Z,0,true",
        "2020-07-04T00:00:00.000Z,2020-07-04T00:00:00.000Z,0,false",
        "2020-07-04T00:00:00.000Z,2020-07-02T23:59:59.999Z,-15,true",
        "2020-07-04T00:00:00.000Z,2020-07-03T23:59:59.999Z,-15,false"
    )
    fun `is covering submission date`(
        periodEndDate: String,
        submissionDate: String,
        offsetMinutes: Long,
        expected: Boolean
    ) {
        DailyZIPSubmissionPeriod(periodEndDate.asInstant())
            .isCoveringSubmissionDate(submissionDate.asInstant(), Duration.ofMinutes(offsetMinutes))
            .run {
                assertThat(this)
                    .describedAs("end date: $periodEndDate submission date: $submissionDate offset: $offsetMinutes")
                    .isEqualTo(expected)
            }
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-04T00:00:00.000Z,distribution/daily/2020070500.zip",
        "2020-07-04T23:59:59.999Z,distribution/daily/2020070500.zip",

        )
    fun `calculates period for submission date`(submissionDate: String, expected: String) {
        DailyZIPSubmissionPeriod.periodForSubmissionDate(submissionDate.asInstant()).run {
            assertThat(zipPath())
                .describedAs("submission date: $submissionDate")
                .isEqualTo(expected)
        }
    }

    @Test
    fun `verify all periods`() {
        val zipSubmissionPeriod = DailyZIPSubmissionPeriod("2020-07-20T00:00:00.000Z".asInstant())
        val allPeriods = zipSubmissionPeriod.allPeriodsToGenerate()

        assertThat(allPeriods.map { it.zipPath() })
            .hasSize(15)
            .containsExactly(
                // Special case: zip "unofficially" available during the day and
                // contains the keys submitted so far (the mobile apps may only
                // download the ZIP after the end of the day)
                "distribution/daily/2020072000.zip",

                // Regular cases (API spec)
                "distribution/daily/2020071900.zip",
                "distribution/daily/2020071800.zip",
                "distribution/daily/2020071700.zip",
                "distribution/daily/2020071600.zip",
                "distribution/daily/2020071500.zip",
                "distribution/daily/2020071400.zip",
                "distribution/daily/2020071300.zip",
                "distribution/daily/2020071200.zip",
                "distribution/daily/2020071100.zip",
                "distribution/daily/2020071000.zip",
                "distribution/daily/2020070900.zip",
                "distribution/daily/2020070800.zip",
                "distribution/daily/2020070700.zip",
                "distribution/daily/2020070600.zip"
            )
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-20T00:00:00.000Z,2020-07-19T00:00:00.000Z",
        "2020-07-01T00:00:00.000Z,2020-06-30T00:00:00.000Z",
        "2021-01-01T00:00:00.000Z,2020-12-31T00:00:00.000Z",
    )
    fun `calculate start date inclusive`(periodEndDate: String, expected: String) {
        DailyZIPSubmissionPeriod(periodEndDate.asInstant()).startInclusive.run {
            assertThat(this).isEqualTo(expected.asInstant())
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "2020-07-20T00:00:00.000Z",
            "2020-07-19T00:00:00.000Z",
            "2020-07-18T00:00:00.000Z"
        ]
    )
    fun `calculate end date inclusive`(input: String) {
        DailyZIPSubmissionPeriod(input.asInstant()).endExclusive.run {
            assertThat(this).isEqualTo(input.asInstant())
        }
    }

    @Test
    fun `verify toString`() {
        assertThat(DailyZIPSubmissionPeriod("2020-07-20T00:00:00.000Z".asInstant()).toString())
            .isEqualTo("1 day: from 2020071900 (inclusive) to 2020072000 (exclusive)")
    }
}
