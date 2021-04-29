package uk.nhs.nhsx.core

import java.time.Clock
import java.time.Instant

typealias Clock = () -> Instant

object SystemClock {
    val CLOCK = Clock.systemUTC()::instant
}
