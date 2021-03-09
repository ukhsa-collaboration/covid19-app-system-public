package uk.nhs.nhsx.virology.result

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.virology.result.TestResult.*

class TestResultTest {
    @Test
    fun `converts from wire value`() {
        assertThat(TestResult.from("POSITIVE"), equalTo(Positive))
        assertThat(TestResult.from("NEGATIVE"), equalTo(Negative))
        assertThat(TestResult.from("VOID"), equalTo(Void))
        assertThat(TestResult.from("INDETERMINATE"), equalTo(Void))
    }
}


