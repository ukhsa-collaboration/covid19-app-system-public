package uk.nhs.nhsx.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.domain.TestResult

class TestResultTest {
    @Test
    fun `converts from wire value`() {
        assertThat(TestResult.from("POSITIVE"), equalTo(TestResult.Positive))
        assertThat(TestResult.from("NEGATIVE"), equalTo(TestResult.Negative))
        assertThat(TestResult.from("VOID"), equalTo(TestResult.Void))
        assertThat(TestResult.from("INDETERMINATE"), equalTo(TestResult.Void))
    }
}
