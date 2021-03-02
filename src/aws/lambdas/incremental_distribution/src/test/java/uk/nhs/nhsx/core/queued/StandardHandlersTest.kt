package uk.nhs.nhsx.core.queued

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.proxy

class StandardHandlersTest {

    @Test
    fun `uncaught exception is logged`() {
        val events = RecordingEvents()
        val runtimeException = RuntimeException("hello")
        val handler = StandardHandlers.catchException(events) { _, _ ->
            throw runtimeException
        }

        assertThat({ handler(SQSEvent(), proxy()) }, throws(equalTo(runtimeException)))

        events.containsExactly(ExceptionThrown::class)
    }
}
