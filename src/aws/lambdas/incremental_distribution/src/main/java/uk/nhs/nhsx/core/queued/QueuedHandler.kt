package uk.nhs.nhsx.core.queued

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.StandardHandlers.logAndRethrowException
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import java.time.Duration

abstract class QueuedHandler(protected val events: Events) : RequestHandler<SQSEvent, String> {

    override fun handleRequest(request: SQSEvent, context: Context): String {
        val start = System.currentTimeMillis()
        events(QueuedEventStarted(javaClass.simpleName))

        return logAndRethrowException(events, handler())(request, context).also {
            events(it)
            events(
                QueuedEventCompleted(javaClass.simpleName, Duration.ofMillis(System.currentTimeMillis() - start))
            )
        }.toString()
    }

    abstract fun handler(): Handler<SQSEvent, Event>
}

data class QueuedEventStarted(val handler: String) : Event(EventCategory.Metric)
data class QueuedEventCompleted(val handler: String, val runtime: Duration) : Event(EventCategory.Metric)
