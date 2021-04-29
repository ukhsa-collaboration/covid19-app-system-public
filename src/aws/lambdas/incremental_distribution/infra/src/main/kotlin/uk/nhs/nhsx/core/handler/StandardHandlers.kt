package uk.nhs.nhsx.core.handler

import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown

fun <T, R> logAndRethrowException(events: Events, fn: Handler<T, R>) = Handler<T, R> { request, context ->
    try {
        fn(request, context)
    } catch (e: Exception) {
        events(ExceptionThrown(e))
        throw e
    }
}
