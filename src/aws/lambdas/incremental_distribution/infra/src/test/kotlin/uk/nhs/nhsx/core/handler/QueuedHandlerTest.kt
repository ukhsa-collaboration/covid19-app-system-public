package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import java.util.UUID

class QueuedHandlerTest {

    private val events = RecordingEvents()

    @Test
    fun `logs events when successful`() {
        object : QueuedHandler(events) {
            override fun handler() = Handler<SQSEvent, Event> { _, _ -> Result }
        }.handleRequest(SQSEvent(), TestContext())

        events.containsExactly(QueuedEventStarted::class, Result::class, QueuedEventCompleted::class)
    }

    @Test
    fun `logs events when exception`() {
        val e = RuntimeException("hello")
        assertThat({
            object : QueuedHandler(events) {
                override fun handler() = Handler<SQSEvent, Event> { _, _ -> throw e }
            }.handleRequest(SQSEvent(), TestContext())
        }, throws(equalTo(e)))

        events.containsExactly(QueuedEventStarted::class, ExceptionThrown::class)
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        val context = TestContext().apply { requestId = uuid }

        object : QueuedHandler(events) {
            override fun handler() = Handler<SQSEvent, Event> { _, _ -> Result }
        }.handleRequest(SQSEvent(), context)

        assertThat(RequestContext.awsRequestId(), equalTo(uuid.toString()))
    }
}
