package uk.nhs.nhsx.core.queued

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import java.time.Duration

abstract class QueuedHandler(protected val events: Events) :
    RequestHandler<SQSEvent, String> {

    override fun handleRequest(request: SQSEvent, context: Context): String {
        val start = System.currentTimeMillis()
        events.emit(javaClass, QueuedEventStarted(javaClass.simpleName))
        return StandardHandlers.catchException(events, handler())(request, context).also {
            events.emit(javaClass, it)
            events.emit(
                javaClass, QueuedEventCompleted(
                    javaClass.simpleName,
                    Duration.ofMillis(System.currentTimeMillis() - start)
                )
            )
        }.toString()
    }

    abstract fun handler(): Queued.Handler
}

data class QueuedEventStarted(val handler: String) : Event(EventCategory.Metric)
data class QueuedEventCompleted(val handler: String, val runtime: Duration) : Event(EventCategory.Metric)
