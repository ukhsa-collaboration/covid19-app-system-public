package uk.nhs.nhsx.analyticsedge.export

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.SchedulingHandler

class TriggerExportHandler(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val service: DataExportService = dataExportService(environment, events)
) : SchedulingHandler(events) {

    override fun handler() = Handler<ScheduledEvent, Event> { _, _ ->
        service.triggerAllQueries()
        ExportTriggered
    }
}

object ExportTriggered : Event(EventCategory.Info)
