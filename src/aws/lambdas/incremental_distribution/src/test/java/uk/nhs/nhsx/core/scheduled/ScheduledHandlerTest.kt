package uk.nhs.nhsx.core.scheduled

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext

class ScheduledHandlerTest {
    @Test
    fun `logs events when successful`() {
        val events = RecordingEvents()
        object : SchedulingHandler(events) {
            override fun handler() = Scheduling.Handler { _, _ -> Result }
        }.handleRequest(ScheduledEvent(), TestContext())

        events.containsExactly(ScheduledEventStarted::class, Result::class, ScheduledEventCompleted::class)
    }

    @Test
    fun `logs events when exception`() {
        val events = RecordingEvents()

        val e = RuntimeException("hello")
        assertThat({
            object : SchedulingHandler(events) {
                override fun handler() = Scheduling.Handler { _, _ -> throw e }
            }.handleRequest(ScheduledEvent(), TestContext())
        }, throws(equalTo(e)))

        events.containsExactly(ScheduledEventStarted::class, ExceptionThrown::class)
    }
}

object Result : Event(EventCategory.Info)
