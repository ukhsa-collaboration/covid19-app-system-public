package uk.nhs.nhsx.virology.result

import dev.forkhandles.values.LocalDateValue
import dev.forkhandles.values.LocalDateValueFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ofPattern

class TestEndDate private constructor(value: LocalDate) : LocalDateValue(value) {
    companion object : LocalDateValueFactory<TestEndDate>(::TestEndDate, fmt = ofPattern("yyyy-MM-dd'T'00:00:00'Z'")) {
        fun of(year: Int, month: Int, day: Int) = of(LocalDate.of(year, month, day))
    }
}
