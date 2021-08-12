package uk.nhs.nhsx.diagnosiskeydist

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.testhelper.data.asInstant
import java.time.Duration

class DistributionServiceWindowTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "2020-07-04T11:45:59.999Z,early start date,false",
            "2020-07-04T11:46:00.000Z,early start date,true",
            "2020-07-04T11:47:59.999Z,late start date,true",
            "2020-07-04T11:48:00.000Z,late start date,false",
            "2020-07-04T12:46:00.000Z,even hour early start date,false",
            "2020-07-04T12:47:00.000Z,even hour late start date,false",
        ]
    )

    fun `determines distribution start time window`(input: String, description: String, expected: Boolean) {
        expectThat(distributionServiceWindow(input).isValidBatchStartDate())
            .describedAs("$description: $input should have invalid start time window")
            .isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "2020-07-04T00:00:00.000Z,2020-07-04T02:00:00.00Z",
            "2020-07-04T01:00:00.000Z,2020-07-04T02:00:00.00Z",
            "2020-07-04T01:59:59.999Z,2020-07-04T02:00:00.00Z",
            "2020-07-04T22:00:00.000Z,2020-07-05T00:00:00.00Z",
            "2020-07-04T23:00:00.000Z,2020-07-05T00:00:00.00Z",
            "2020-07-04T23:59:59.999Z,2020-07-05T00:00:00.00Z",
        ]
    )

    fun `calculates next window`(input: String, expected: String) {
        val nextWindow = distributionServiceWindow(input)
            .nextWindow()

        expectThat(nextWindow)
            .isEqualTo(expected.asInstant())
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "2020-07-04T00:00:00.000Z,2020-07-04T04:00:00.00Z",
            "2020-07-04T01:00:00.000Z,2020-07-04T04:00:00.00Z",
            "2020-07-04T01:59:59.999Z,2020-07-04T04:00:00.00Z",
            "2020-07-04T22:00:00.000Z,2020-07-05T02:00:00.00Z",
            "2020-07-04T23:00:00.000Z,2020-07-05T02:00:00.00Z",
            "2020-07-04T23:59:59.999Z,2020-07-05T02:00:00.00Z",
        ]
    )

    fun `calculates zip extraction exclusive time`(input: String, expected: String) {
        val zipExpirationExclusive = distributionServiceWindow(input)
            .zipExpirationExclusive()

        expectThat(zipExpirationExclusive)
            .isEqualTo(expected.asInstant())
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "2020-07-04T00:00:00.000Z,2020-07-04T01:46:00.00Z",
            "2020-07-04T01:00:00.000Z,2020-07-04T01:46:00.00Z",
            "2020-07-04T01:59:59.999Z,2020-07-04T01:46:00.00Z",
            "2020-07-04T22:00:00.000Z,2020-07-04T23:46:00.00Z",
            "2020-07-04T23:00:00.000Z,2020-07-04T23:46:00.00Z",
            "2020-07-04T23:59:59.999Z,2020-07-04T23:46:00.00Z",
        ]
    )

    fun `calculates earliest batch start date within hour (inclusive)`(input: String, expected: String) {
        val withinHourInclusive = distributionServiceWindow(input)
            .earliestBatchStartDateWithinHourInclusive()

        expectThat(withinHourInclusive)
            .isEqualTo(expected.asInstant())
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "2020-07-04T00:00:00.000Z,2020-07-04T01:48:00.00Z",
            "2020-07-04T01:00:00.000Z,2020-07-04T01:48:00.00Z",
            "2020-07-04T01:59:59.999Z,2020-07-04T01:48:00.00Z",
            "2020-07-04T22:00:00.000Z,2020-07-04T23:48:00.00Z",
            "2020-07-04T23:00:00.000Z,2020-07-04T23:48:00.00Z",
            "2020-07-04T23:59:59.999Z,2020-07-04T23:48:00.00Z",
        ]
    )
    fun `calculates latest batch start date within hour (exclusive)`(input: String, expected: String) {
        val withinHourExclusive = distributionServiceWindow(input)
            .latestBatchStartDateWithinHourExclusive()

        expectThat(withinHourExclusive)
            .isEqualTo(expected.asInstant())
    }

    private fun distributionServiceWindow(input: String) = DistributionServiceWindow(
        input.asInstant(),
        Duration.ofMinutes(-15)
    )
}
