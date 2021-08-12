package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import java.util.*

class ScheduledHandlerTest {

    private val events = RecordingEvents()

    @Test
    fun `logs events when successful`() {
        object : SchedulingHandler(events) {
            override fun handler() = Handler<ScheduledEvent, Event> { _, _ -> Result }
        }.handleRequest(ScheduledEvent(), TestContext())

        expectThat(events).containsExactly(
            ScheduledEventStarted::class,
            Result::class,
            ScheduledEventCompleted::class
        )
    }

    @Test
    fun `logs events when exception`() {
        expectThrows<RuntimeException> {
            object : SchedulingHandler(events) {
                override fun handler() = Handler<ScheduledEvent, Event> { _, _ -> throw RuntimeException("hello") }
            }.handleRequest(ScheduledEvent(), TestContext())
        }.message.isEqualTo("hello")

        expectThat(events).containsExactly(ScheduledEventStarted::class, ExceptionThrown::class)
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        val context = TestContext().apply { requestId = uuid }

        object : SchedulingHandler(events) {
            override fun handler() = Handler<ScheduledEvent, Event> { _, _ -> Result }
        }.handleRequest(ScheduledEvent(), context)

        expectThat(RequestContext.awsRequestId()).isEqualTo(uuid.toString())
    }
}

object Result : Event(EventCategory.Info)
