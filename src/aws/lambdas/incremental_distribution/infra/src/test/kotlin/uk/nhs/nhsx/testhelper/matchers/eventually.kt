package uk.nhs.nhsx.testhelper.matchers

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import org.awaitility.Awaitility
import java.time.Duration

fun <T> eventually(
    matcher: Matcher<T>,
    ofSeconds: Duration = Duration.ofSeconds(10),
    fn: () -> T
) {
    Awaitility.await()
        .atMost(ofSeconds)
        .until {
            matcher(fn()) == MatchResult.Match
        }
}

