package uk.nhs.nhsx.core.scheduled

import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown

object StandardHandlers {
    fun catchException(events: Events, fn: Scheduling.Handler) = Scheduling.Handler { event, context ->
        try {
            fn(event, context)
        } catch (e: Exception) {
            events.emit(javaClass, ExceptionThrown(e))
            throw e
        }
    }
}
