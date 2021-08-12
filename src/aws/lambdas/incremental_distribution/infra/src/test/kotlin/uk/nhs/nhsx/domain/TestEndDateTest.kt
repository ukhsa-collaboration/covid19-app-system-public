package uk.nhs.nhsx.domain

import dev.forkhandles.values.parseOrNull
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.time.LocalDate

class TestEndDateTest {

    @Test
    fun `cannot construct from non-midnight date`() {
        expectThat(TestEndDate.parseOrNull("")).isNull()
        expectThat(TestEndDate.parseOrNull("2020-04-23T00:00:01Z")).isNull()
    }

    @Test
    fun `can construct legal date from ISO instant string`() {
        expectThat(TestEndDate.parseOrNull("2020-04-23T00:00:00Z")).isEqualTo(TestEndDate.of(LocalDate.of(2020, 4, 23)))
    }

    @Test
    fun `shows as ISO instant`() {
        expectThat(TestEndDate.show(TestEndDate.of(LocalDate.of(2020, 4, 23)))).isEqualTo("2020-04-23T00:00:00Z")
    }
}
