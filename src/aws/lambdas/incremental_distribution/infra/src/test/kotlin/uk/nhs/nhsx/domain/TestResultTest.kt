package uk.nhs.nhsx.domain

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void

class TestResultTest {

    @Test
    fun `converts from wire value`() {
        expectThat(TestResult.from("POSITIVE")).isEqualTo(Positive)
        expectThat(TestResult.from("NEGATIVE")).isEqualTo(Negative)
        expectThat(TestResult.from("VOID")).isEqualTo(Void)
        expectThat(TestResult.from("INDETERMINATE")).isEqualTo(Void)
    }
}
