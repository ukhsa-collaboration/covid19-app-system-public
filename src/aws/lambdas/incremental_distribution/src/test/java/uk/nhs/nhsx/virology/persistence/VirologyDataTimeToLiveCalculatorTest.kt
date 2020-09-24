package uk.nhs.nhsx.virology.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

class VirologyDataTimeToLiveCalculatorTest {

    @Test
    fun `calculate time to live using duration of seconds`() {
        val timeToLive = VirologyDataTimeToLiveCalculator(
            Duration.ofSeconds(123),
            Duration.ofSeconds(456)
        ).apply { Instant.ofEpochSecond(0) }

        assertThat(timeToLive.testDataExpireAt).isEqualTo(123)
        assertThat(timeToLive.submissionDataExpireAt).isEqualTo(456)
    }

    @Test
    fun `calculate time to live using duration of days`() {
        val timeToLive = VirologyDataTimeToLiveCalculator(
            Duration.ofDays(1),
            Duration.ofDays(2)
        ).apply { Instant.ofEpochSecond(0) }

        assertThat(timeToLive.testDataExpireAt).isEqualTo(24 * 60 * 60)
        assertThat(timeToLive.submissionDataExpireAt).isEqualTo(2 * 24 * 60 * 60)
    }

    @Test
    fun `calculate time to live using duration of hours`() {
        val timeToLive = VirologyDataTimeToLiveCalculator(
            Duration.ofHours(12),
            Duration.ofHours(24)
        ).apply { Instant.ofEpochSecond(0) }

        assertThat(timeToLive.testDataExpireAt).isEqualTo(12 * 60 * 60)
        assertThat(timeToLive.submissionDataExpireAt).isEqualTo(24 * 60 * 60)
    }

}