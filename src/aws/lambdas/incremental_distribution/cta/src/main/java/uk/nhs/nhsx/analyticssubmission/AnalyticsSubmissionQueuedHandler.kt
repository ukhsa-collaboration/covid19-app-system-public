package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.QueuedHandler

object QueueEntryProcessed : Event(EventCategory.Operational)

class AnalyticsSubmissionQueuedHandler constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = SystemClock.CLOCK,
    events: Events = PrintingJsonEvents(clock),
    analyticsSubmissionHandler: AnalyticsSubmissionHandler = AnalyticsSubmissionHandler(environment, clock, events)
) : QueuedHandler(events) {

    private val handler = Handler<SQSEvent, Event> { input, context ->
        analyticsSubmissionHandler.handleRequest(generateRequestEvent(input), context)
        QueueEntryProcessed
    }

    override fun handler() = handler

    private fun generateRequestEvent(input: SQSEvent): APIGatewayProxyRequestEvent {
        if (input.records.size != 1) {
            throw IllegalStateException(".tf configuration error: batch_size != 1")
        }

        return Json.readJsonOrThrow(input.records[0].body)
    }
}
