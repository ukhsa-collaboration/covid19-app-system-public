package uk.nhs.nhsx.diagnosiskeydist.apispec

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.Instant

class TwoHourlyZIPSubmissionPeriodTest {

    @ParameterizedTest
    @CsvSource(
        "2020-07-20T00:00:00.000Z,false",
        "2020-07-20T02:00:00.000Z,false",
        "2020-07-20T05:00:00.000Z,true",
        "2020-07-20T00:05:00.000Z,true",
        "2020-07-20T00:00:05.000Z,true",
        "2020-07-20T00:00:00.005Z,true",
        "2020-07-20T05:05:05.005Z,true"
    )
    fun `throws exception if submission period is invalid`(periodEndDate: String, throwsEx: Boolean) {
        val endDate = periodEndDate.asInstant()

        if (throwsEx) {
            assertThatThrownBy { TwoHourlyZIPSubmissionPeriod(endDate) }.isInstanceOf(IllegalStateException::class.java)
        } else {
            assertThat(TwoHourlyZIPSubmissionPeriod(endDate)).isNotNull
        }
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-20T16:00:00.000Z,distribution/two-hourly/2020072016.zip"
    )
    fun `creates ZIP path`(periodEndDate: String, expected: String) {
        TwoHourlyZIPSubmissionPeriod(periodEndDate.asInstant()).run {
            assertThat(zipPath()).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-04T00:00:00.000Z,2020-07-03T21:59:59.999Z,0,false",
        "2020-07-04T00:00:00.000Z,2020-07-03T22:00:00.000Z,0,true",
        "2020-07-04T00:00:00.000Z,2020-07-03T23:59:59.999Z,0,true",
        "2020-07-04T00:00:00.000Z,2020-07-04T00:00:00.000Z,0,false"
    )
    fun `is covering submission date`(
        periodEndDate: String,
        submissionDate: String,
        offsetMinutes: Long,
        expected: Boolean
    ) {
        TwoHourlyZIPSubmissionPeriod(periodEndDate.asInstant())
            .isCoveringSubmissionDate(submissionDate.asInstant(), Duration.ofMinutes(offsetMinutes))
            .run {
                assertThat(this)
                    .describedAs("end date: $periodEndDate submission date: $submissionDate offset: $offsetMinutes")
                    .isEqualTo(expected)
            }
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-04T00:00:00.000Z,distribution/two-hourly/2020070402.zip",
        "2020-07-04T23:59:59.999Z,distribution/two-hourly/2020070500.zip",
    )
    fun `calculates period for submission date`(submissionDate: String, expected: String) {
        TwoHourlyZIPSubmissionPeriod.periodForSubmissionDate(submissionDate.asInstant()).run {
            assertThat(zipPath())
                .describedAs("submission date: $submissionDate")
                .isEqualTo(expected)
        }
    }

    @Test
    fun `verify all periods`() {
        val zipSubmissionPeriod = TwoHourlyZIPSubmissionPeriod("2020-07-20T04:00:00.000Z".asInstant())
        val allPeriods = zipSubmissionPeriod.allPeriodsToGenerate()

        assertThat(allPeriods.map { it.zipPath() })
            .hasSize(168)
            .containsExactly(
                "distribution/two-hourly/2020072004.zip",
                "distribution/two-hourly/2020072002.zip",
                "distribution/two-hourly/2020072000.zip",
                "distribution/two-hourly/2020071922.zip",
                "distribution/two-hourly/2020071920.zip",
                "distribution/two-hourly/2020071918.zip",
                "distribution/two-hourly/2020071916.zip",
                "distribution/two-hourly/2020071914.zip",
                "distribution/two-hourly/2020071912.zip",
                "distribution/two-hourly/2020071910.zip",
                "distribution/two-hourly/2020071908.zip",
                "distribution/two-hourly/2020071906.zip",
                "distribution/two-hourly/2020071904.zip",
                "distribution/two-hourly/2020071902.zip",
                "distribution/two-hourly/2020071900.zip",
                "distribution/two-hourly/2020071822.zip",
                "distribution/two-hourly/2020071820.zip",
                "distribution/two-hourly/2020071818.zip",
                "distribution/two-hourly/2020071816.zip",
                "distribution/two-hourly/2020071814.zip",
                "distribution/two-hourly/2020071812.zip",
                "distribution/two-hourly/2020071810.zip",
                "distribution/two-hourly/2020071808.zip",
                "distribution/two-hourly/2020071806.zip",
                "distribution/two-hourly/2020071804.zip",
                "distribution/two-hourly/2020071802.zip",
                "distribution/two-hourly/2020071800.zip",
                "distribution/two-hourly/2020071722.zip",
                "distribution/two-hourly/2020071720.zip",
                "distribution/two-hourly/2020071718.zip",
                "distribution/two-hourly/2020071716.zip",
                "distribution/two-hourly/2020071714.zip",
                "distribution/two-hourly/2020071712.zip",
                "distribution/two-hourly/2020071710.zip",
                "distribution/two-hourly/2020071708.zip",
                "distribution/two-hourly/2020071706.zip",
                "distribution/two-hourly/2020071704.zip",
                "distribution/two-hourly/2020071702.zip",
                "distribution/two-hourly/2020071700.zip",
                "distribution/two-hourly/2020071622.zip",
                "distribution/two-hourly/2020071620.zip",
                "distribution/two-hourly/2020071618.zip",
                "distribution/two-hourly/2020071616.zip",
                "distribution/two-hourly/2020071614.zip",
                "distribution/two-hourly/2020071612.zip",
                "distribution/two-hourly/2020071610.zip",
                "distribution/two-hourly/2020071608.zip",
                "distribution/two-hourly/2020071606.zip",
                "distribution/two-hourly/2020071604.zip",
                "distribution/two-hourly/2020071602.zip",
                "distribution/two-hourly/2020071600.zip",
                "distribution/two-hourly/2020071522.zip",
                "distribution/two-hourly/2020071520.zip",
                "distribution/two-hourly/2020071518.zip",
                "distribution/two-hourly/2020071516.zip",
                "distribution/two-hourly/2020071514.zip",
                "distribution/two-hourly/2020071512.zip",
                "distribution/two-hourly/2020071510.zip",
                "distribution/two-hourly/2020071508.zip",
                "distribution/two-hourly/2020071506.zip",
                "distribution/two-hourly/2020071504.zip",
                "distribution/two-hourly/2020071502.zip",
                "distribution/two-hourly/2020071500.zip",
                "distribution/two-hourly/2020071422.zip",
                "distribution/two-hourly/2020071420.zip",
                "distribution/two-hourly/2020071418.zip",
                "distribution/two-hourly/2020071416.zip",
                "distribution/two-hourly/2020071414.zip",
                "distribution/two-hourly/2020071412.zip",
                "distribution/two-hourly/2020071410.zip",
                "distribution/two-hourly/2020071408.zip",
                "distribution/two-hourly/2020071406.zip",
                "distribution/two-hourly/2020071404.zip",
                "distribution/two-hourly/2020071402.zip",
                "distribution/two-hourly/2020071400.zip",
                "distribution/two-hourly/2020071322.zip",
                "distribution/two-hourly/2020071320.zip",
                "distribution/two-hourly/2020071318.zip",
                "distribution/two-hourly/2020071316.zip",
                "distribution/two-hourly/2020071314.zip",
                "distribution/two-hourly/2020071312.zip",
                "distribution/two-hourly/2020071310.zip",
                "distribution/two-hourly/2020071308.zip",
                "distribution/two-hourly/2020071306.zip",
                "distribution/two-hourly/2020071304.zip",
                "distribution/two-hourly/2020071302.zip",
                "distribution/two-hourly/2020071300.zip",
                "distribution/two-hourly/2020071222.zip",
                "distribution/two-hourly/2020071220.zip",
                "distribution/two-hourly/2020071218.zip",
                "distribution/two-hourly/2020071216.zip",
                "distribution/two-hourly/2020071214.zip",
                "distribution/two-hourly/2020071212.zip",
                "distribution/two-hourly/2020071210.zip",
                "distribution/two-hourly/2020071208.zip",
                "distribution/two-hourly/2020071206.zip",
                "distribution/two-hourly/2020071204.zip",
                "distribution/two-hourly/2020071202.zip",
                "distribution/two-hourly/2020071200.zip",
                "distribution/two-hourly/2020071122.zip",
                "distribution/two-hourly/2020071120.zip",
                "distribution/two-hourly/2020071118.zip",
                "distribution/two-hourly/2020071116.zip",
                "distribution/two-hourly/2020071114.zip",
                "distribution/two-hourly/2020071112.zip",
                "distribution/two-hourly/2020071110.zip",
                "distribution/two-hourly/2020071108.zip",
                "distribution/two-hourly/2020071106.zip",
                "distribution/two-hourly/2020071104.zip",
                "distribution/two-hourly/2020071102.zip",
                "distribution/two-hourly/2020071100.zip",
                "distribution/two-hourly/2020071022.zip",
                "distribution/two-hourly/2020071020.zip",
                "distribution/two-hourly/2020071018.zip",
                "distribution/two-hourly/2020071016.zip",
                "distribution/two-hourly/2020071014.zip",
                "distribution/two-hourly/2020071012.zip",
                "distribution/two-hourly/2020071010.zip",
                "distribution/two-hourly/2020071008.zip",
                "distribution/two-hourly/2020071006.zip",
                "distribution/two-hourly/2020071004.zip",
                "distribution/two-hourly/2020071002.zip",
                "distribution/two-hourly/2020071000.zip",
                "distribution/two-hourly/2020070922.zip",
                "distribution/two-hourly/2020070920.zip",
                "distribution/two-hourly/2020070918.zip",
                "distribution/two-hourly/2020070916.zip",
                "distribution/two-hourly/2020070914.zip",
                "distribution/two-hourly/2020070912.zip",
                "distribution/two-hourly/2020070910.zip",
                "distribution/two-hourly/2020070908.zip",
                "distribution/two-hourly/2020070906.zip",
                "distribution/two-hourly/2020070904.zip",
                "distribution/two-hourly/2020070902.zip",
                "distribution/two-hourly/2020070900.zip",
                "distribution/two-hourly/2020070822.zip",
                "distribution/two-hourly/2020070820.zip",
                "distribution/two-hourly/2020070818.zip",
                "distribution/two-hourly/2020070816.zip",
                "distribution/two-hourly/2020070814.zip",
                "distribution/two-hourly/2020070812.zip",
                "distribution/two-hourly/2020070810.zip",
                "distribution/two-hourly/2020070808.zip",
                "distribution/two-hourly/2020070806.zip",
                "distribution/two-hourly/2020070804.zip",
                "distribution/two-hourly/2020070802.zip",
                "distribution/two-hourly/2020070800.zip",
                "distribution/two-hourly/2020070722.zip",
                "distribution/two-hourly/2020070720.zip",
                "distribution/two-hourly/2020070718.zip",
                "distribution/two-hourly/2020070716.zip",
                "distribution/two-hourly/2020070714.zip",
                "distribution/two-hourly/2020070712.zip",
                "distribution/two-hourly/2020070710.zip",
                "distribution/two-hourly/2020070708.zip",
                "distribution/two-hourly/2020070706.zip",
                "distribution/two-hourly/2020070704.zip",
                "distribution/two-hourly/2020070702.zip",
                "distribution/two-hourly/2020070700.zip",
                "distribution/two-hourly/2020070622.zip",
                "distribution/two-hourly/2020070620.zip",
                "distribution/two-hourly/2020070618.zip",
                "distribution/two-hourly/2020070616.zip",
                "distribution/two-hourly/2020070614.zip",
                "distribution/two-hourly/2020070612.zip",
                "distribution/two-hourly/2020070610.zip",
                "distribution/two-hourly/2020070608.zip",
                "distribution/two-hourly/2020070606.zip"
            )
    }

    @Test
    fun `calculates endExclusive (almost midnight)`() {
        val almostMidnight = "2020-07-03T23:59:59.999Z".asInstant()
        val midnight = TwoHourlyZIPSubmissionPeriod
            .periodForSubmissionDate(almostMidnight)

        assertThat(almostMidnight.plusMillis(1))
            .isEqualTo(midnight.endExclusive)
    }

    @Test
    fun `calculates endExclusive (after midnight)`() {
        val afterMidnight = "2020-07-03T00:01:00.000Z".asInstant()
        val twoInTheMorning = TwoHourlyZIPSubmissionPeriod
            .periodForSubmissionDate(afterMidnight)

        assertThat(
            afterMidnight
                .plus(Duration.ofMinutes(59))
                .plus(Duration.ofHours(1))
        ).isEqualTo(twoInTheMorning.endExclusive)
    }

    @ParameterizedTest
    @CsvSource(
        "2020-07-20T00:00:00.000Z,2020-07-19T22:00:00.000Z",
        "2020-07-01T00:00:00.000Z,2020-06-30T22:00:00.000Z",
    )
    fun `calculate start date inclusive`(periodEndDate: String, expected: String) {
        TwoHourlyZIPSubmissionPeriod(periodEndDate.asInstant()).startInclusive.run {
            assertThat(this).isEqualTo(expected.asInstant())
        }
    }

    @Test
    fun `verify toString`() {
        assertThat(TwoHourlyZIPSubmissionPeriod("2020-07-20T00:00:00.000Z".asInstant()).toString())
            .isEqualTo("2 hours: from 2020071922 (inclusive) to 2020072000 (exclusive)")
    }

    private fun String.asInstant() = Instant.parse(this)
}
