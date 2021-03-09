package uk.nhs.nhsx.virology.result

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.parseOrNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestEndDateTest {

    @Test
    fun `cannot construct from non-midnight date`() {
        assertThat(TestEndDate.parseOrNull(""), absent())
        assertThat(TestEndDate.parseOrNull("2020-04-23T00:00:01Z"), absent())
    }

    @Test
    fun `can construct legal date from ISO instant string`() {
        assertThat(TestEndDate.parseOrNull("2020-04-23T00:00:00Z"), equalTo(TestEndDate.of(LocalDate.of(2020, 4, 23))))
    }

    @Test
    fun `shows as ISO instant`() {
        assertThat(TestEndDate.show(TestEndDate.of(LocalDate.of(2020, 4, 23))), equalTo("2020-04-23T00:00:00Z"))
    }
}
