package uk.nhs.nhsx.core.scheduled

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import uk.nhs.nhsx.core.events.Event

interface Scheduling {
    fun interface Handler : (ScheduledEvent, Context) -> Event
}
