package uk.nhs.nhsx.virology.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestResultTest {

    @Test
    fun `returns true when positive`() {
        val testResult = TestResult("some-token", "some-date", "POSITIVE", "available")
        assertThat(testResult.isPositive).isTrue()
    }

    @Test
    fun `returns false when result negative`() {
        val testResult = TestResult("some-token", "some-date", "NEGATIVE", "available")
        assertThat(testResult.isPositive).isFalse()
    }

    @Test
    fun `returns true when status available`() {
        val testResult = TestResult("some-token", "some-date", "NEGATIVE", "available")
        assertThat(testResult.isAvailable).isTrue()
    }

    @Test
    fun `returns false when status pending`() {
        val testResult = TestResult("some-token", "some-date", "NEGATIVE", "pending")
        assertThat(testResult.isAvailable).isFalse()
    }
}