package uk.nhs.nhsx.core.handler

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext

class StandardHandlersTest {

    @Test
    fun `uncaught exception is logged and rethrown`() {
        val events = RecordingEvents()
        val runtimeException = RuntimeException("hello")
        val handler = logAndRethrowException<String, String>(events) { _, _ -> throw runtimeException }

        assertThat({ handler("", TestContext()) }, throws(equalTo(runtimeException)))

        events.containsExactly(ExceptionThrown::class)
    }
}
