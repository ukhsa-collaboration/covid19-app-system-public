package uk.nhs.nhsx.core

import java.time.Clock
import java.util.function.Supplier

object SystemClock {
    @JvmField
    val CLOCK = Supplier { Clock.systemUTC().instant() }
}
