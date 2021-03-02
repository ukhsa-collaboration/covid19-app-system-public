package uk.nhs.nhsx.core.queued

import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown

object StandardHandlers {
    fun catchException(events: Events, fn: Queued.Handler) = Queued.Handler { event, context ->
        try {
            fn(event, context)
        } catch (e: Exception) {
            events.emit(javaClass, ExceptionThrown(e))
            throw e
        }
    }
}
