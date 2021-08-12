package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import java.util.*

class QueuedHandlerTest {

    private val events = RecordingEvents()

    @Test
    fun `logs events when successful`() {
        object : QueuedHandler(events) {
            override fun handler() = Handler<SQSEvent, Event> { _, _ -> Result }
        }.handleRequest(SQSEvent(), TestContext())

        expectThat(events).containsExactly(
            QueuedEventStarted::class,
            Result::class,
            QueuedEventCompleted::class
        )
    }

    @Test
    fun `logs events when exception`() {
        expectThrows<RuntimeException> {
            object : QueuedHandler(events) {
                override fun handler() = Handler<SQSEvent, Event> { _, _ -> throw RuntimeException("hello") }
            }.handleRequest(SQSEvent(), TestContext())
        }.message.isEqualTo("hello")

        expectThat(events).containsExactly(
            QueuedEventStarted::class,
            ExceptionThrown::class
        )
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        val context = TestContext().apply { requestId = uuid }

        object : QueuedHandler(events) {
            override fun handler() = Handler<SQSEvent, Event> { _, _ -> Result }
        }.handleRequest(SQSEvent(), context)

        expectThat(RequestContext.awsRequestId()).isEqualTo(uuid.toString())
    }
}
