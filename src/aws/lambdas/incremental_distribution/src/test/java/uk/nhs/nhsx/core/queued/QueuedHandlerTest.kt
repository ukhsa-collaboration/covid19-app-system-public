package uk.nhs.nhsx.core.queued

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext

class QueuedHandlerTest {
    @Test
    fun `logs events when successful`() {
        val events = RecordingEvents()
        object : QueuedHandler(events) {
            override fun handler() = Queued.Handler { _, _ -> Result }
        }.handleRequest(SQSEvent(), TestContext())

        events.containsExactly(QueuedEventStarted::class, Result::class, QueuedEventCompleted::class)
    }

    @Test
    fun `logs events when exception`() {
        val events = RecordingEvents()

        val e = RuntimeException("hello")
        assertThat({
            object : QueuedHandler(events) {
                override fun handler() = Queued.Handler { _, _ -> throw e }
            }.handleRequest(SQSEvent(), TestContext())
        }, throws(equalTo(e)))

        events.containsExactly(QueuedEventStarted::class, ExceptionThrown::class)
    }
}

object Result : Event(EventCategory.Info)
