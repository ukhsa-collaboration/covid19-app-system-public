package uk.nhs.nhsx.testhelper.matchers

import org.awaitility.Awaitility
import java.time.Duration

fun <T> eventually(
    matcher: (T) -> Boolean,
    ofSeconds: Duration = Duration.ofSeconds(10),
    fn: () -> T
) {
    Awaitility.await()
        .atMost(ofSeconds)
        .until { matcher(fn()) }
}

