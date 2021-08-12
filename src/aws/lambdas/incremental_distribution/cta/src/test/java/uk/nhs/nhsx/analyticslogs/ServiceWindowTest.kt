package uk.nhs.nhsx.analyticslogs

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ServiceWindowTest {

    @Test
    fun `return correct window when time is 1am`() {
        val instantSetTo1am = Instant.from(ZonedDateTime.of(2021, 1, 2, 1, 0, 0, 0, ZoneOffset.UTC))
        val expectedStartTime = Instant.from(ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
        val expectedEndTime = Instant.from(ZonedDateTime.of(2021, 1, 1, 23, 59, 59, 0, ZoneOffset.UTC))
        val window = ServiceWindow(instantSetTo1am)

        expectThat(window.queryStart()).isEqualTo(expectedStartTime.epochSecond)
        expectThat(window.queryEnd()).isEqualTo(expectedEndTime.epochSecond)
    }

    @Test
    fun `return correct window when time is 6am`() {
        val instantSetTo6am = Instant.from(ZonedDateTime.of(2021, 1, 2, 6, 0, 0, 0, ZoneOffset.UTC))
        val expectedStartTime = Instant.from(ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
        val expectedEndTime = Instant.from(ZonedDateTime.of(2021, 1, 1, 23, 59, 59, 0, ZoneOffset.UTC))
        val window = ServiceWindow(instantSetTo6am)

        expectThat(window.queryStart()).isEqualTo(expectedStartTime.epochSecond)
        expectThat(window.queryEnd()).isEqualTo(expectedEndTime.epochSecond)
    }
}
