package uk.nhs.nhsx.testhelper.assertions

import strikt.api.Assertion
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import java.time.Instant

fun <T : Event> Assertion.Builder<T>.isSameAsJson(expected: String) = apply {
    asJsonString.isEqualToJson(expected)
}

val <T : Event> Assertion.Builder<T>.asJsonString
    get() = get("event as JSON") {
        val out = StringBuilder()
        val events = PrintingJsonEvents({ Instant.EPOCH }, out::append)
        events(this)
        out.toString()
    }
