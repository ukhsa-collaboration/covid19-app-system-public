package uk.nhs.nhsx.analyticslogs

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC

class ServiceWindow(time: Instant) {
    private val yesterday: LocalDate = LocalDate.ofInstant(time, UTC).minusDays(1)
    fun queryStart() = yesterday.atStartOfDay().toEpochSecond(UTC)
    fun queryEnd() = yesterday.atTime(23, 59, 59, 0).toEpochSecond(UTC)
}
