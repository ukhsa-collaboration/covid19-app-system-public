package uk.nhs.nhsx.testhelper.junit

import smoke.env.Sleeper.Companion.Real
import java.time.Duration
import java.time.LocalDateTime

fun assertWithin(timeout: Duration, interval: Duration = Duration.ofSeconds(1), fn: () -> Unit) {
    val start = LocalDateTime.now()
    val end = start + timeout
    while (LocalDateTime.now().isBefore(end)) {
        try {
            fn()
        } catch (e: AssertionError) {
            if (LocalDateTime.now().isAfter(end)) throw e
        }
        Real(interval)
    }
}