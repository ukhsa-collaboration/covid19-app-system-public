package uk.nhs.nhsx.virology.persistence

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.testhelper.data.asInstant
import java.time.Duration
import java.time.Instant

class VirologyDataTimeToLiveCalculatorTest {

    private val now = Instant.EPOCH

    @Test
    fun `calculate time to live using duration of seconds`() {
        val timeToLive = VirologyDataTimeToLiveCalculator(
            Duration.ofSeconds(123),
            Duration.ofSeconds(456)
        ).invoke { now }

        expectThat(timeToLive) {
            get(VirologyDataTimeToLive::testDataExpireAt).isEqualTo("1970-01-01T00:02:03Z".asInstant())
            get(VirologyDataTimeToLive::submissionDataExpireAt).isEqualTo("1970-01-01T00:07:36Z".asInstant())
        }
    }

    @Test
    fun `calculate time to live using duration of days`() {
        val timeToLive = VirologyDataTimeToLiveCalculator(
            Duration.ofDays(1),
            Duration.ofDays(2)
        ).invoke { now }

        expectThat(timeToLive) {
            get(VirologyDataTimeToLive::testDataExpireAt).isEqualTo("1970-01-02T00:00:00Z".asInstant())
            get(VirologyDataTimeToLive::submissionDataExpireAt).isEqualTo("1970-01-03T00:00:00Z".asInstant())
        }
    }

    @Test
    fun `calculate time to live using duration of hours`() {
        val timeToLive = VirologyDataTimeToLiveCalculator(
            Duration.ofHours(12),
            Duration.ofHours(24)
        ).invoke { now }

        expectThat(timeToLive) {
            get(VirologyDataTimeToLive::testDataExpireAt).isEqualTo("1970-01-01T12:00:00Z".asInstant())
            get(VirologyDataTimeToLive::submissionDataExpireAt).isEqualTo("1970-01-02T00:00:00Z".asInstant())
        }
    }
}
