package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import java.util.UUID

class ScheduledHandlerTest {
    private val events = RecordingEvents()

    @Test
    fun `logs events when successful`() {
        object : SchedulingHandler(events) {
            override fun handler() = Handler<ScheduledEvent, Event> { _, _ -> Result }
        }.handleRequest(ScheduledEvent(), TestContext())

        events.containsExactly(ScheduledEventStarted::class, Result::class, ScheduledEventCompleted::class)
    }

    @Test
    fun `logs events when exception`() {
        val e = RuntimeException("hello")
        assertThat({
            object : SchedulingHandler(events) {
                override fun handler() = Handler<ScheduledEvent, Event> { _, _ -> throw e }
            }.handleRequest(ScheduledEvent(), TestContext())
        }, throws(equalTo(e)))

        events.containsExactly(ScheduledEventStarted::class, ExceptionThrown::class)
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        val context = TestContext().apply { requestId = uuid }

        object : SchedulingHandler(events) {
            override fun handler() = Handler<ScheduledEvent, Event> { _, _ -> Result }
        }.handleRequest(ScheduledEvent(), context)

        assertThat(RequestContext.awsRequestId(), equalTo(uuid.toString()))
    }
}

object Result : Event(EventCategory.Info)
