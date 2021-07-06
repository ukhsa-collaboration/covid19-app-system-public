package uk.nhs.nhsx.testhelper.assertions

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

fun assertWithin(
    timeout: Duration,
    interval: Duration = Duration.ofSeconds(5),
    fn: () -> Unit
) {
    val start = LocalDateTime.now()
    val end = start + timeout
    val last = AtomicReference<AssertionError>(null)

    while (LocalDateTime.now().isBefore(end)) {
        try {
            return fn()
        } catch (e: AssertionError) {
            last.set(e)
        }
        ThreadSleeper(interval)
    }

    throw last.get()
}

interface Sleeper : (Duration) -> Unit

object ThreadSleeper : Sleeper {
    override fun invoke(duration: Duration) = Thread.sleep(duration.toMillis())
}
