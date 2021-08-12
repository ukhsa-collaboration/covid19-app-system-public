package uk.nhs.nhsx.core.handler

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.containsExactly

class StandardHandlersTest {

    @Test
    fun `uncaught exception is logged and rethrown`() {
        val events = RecordingEvents()
        val runtimeException = RuntimeException("hello")
        val handler = logAndRethrowException<String, String>(events) { _, _ -> throw runtimeException }

        expectThrows<RuntimeException> { handler("", TestContext()) }

        expectThat(events).containsExactly(ExceptionThrown::class)
    }
}
