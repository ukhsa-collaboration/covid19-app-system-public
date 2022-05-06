package uk.nhs.nhsx.pubdash.handlers

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.QueuedHandler
import uk.nhs.nhsx.pubdash.DataExportService
import uk.nhs.nhsx.pubdash.QueueMessage
import uk.nhs.nhsx.pubdash.dataExportService

class DataExportHandler(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val service: DataExportService = dataExportService(environment, events, clock)
) : QueuedHandler(events) {

    override fun handler() = Handler<SQSEvent, Event> { event, _ ->
        if (event.records.size != 1) return@Handler DataExportFailed("Expecting only 1 record, got: ${event.records}")
        val queuedMessage = Json.readJsonOrThrow<QueueMessage>(event.records.first().body)
        service.export(queuedMessage)
        DataExportHandled
    }
}

object DataExportHandled : Event(EventCategory.Info)
data class DataExportFailed(val message: String) : Event(EventCategory.Error)
