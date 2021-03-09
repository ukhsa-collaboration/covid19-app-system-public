package uk.nhs.nhsx.core

import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown

object StandardHandlers {
    fun <T, R> logAndRethrowException(events: Events, fn: Handler<T, R>) = Handler<T, R> { request, context ->
        try {
            fn(request, context)
        } catch (e: Exception) {
            events.emit(javaClass, ExceptionThrown(e))
            throw e
        }
    }
}
