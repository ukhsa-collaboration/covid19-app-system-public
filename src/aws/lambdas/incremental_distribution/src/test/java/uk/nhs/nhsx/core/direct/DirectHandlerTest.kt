package uk.nhs.nhsx.core.direct

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext

class DirectHandlerTest {

    private val recordingEvents = RecordingEvents()

    private val handler = object : DirectHandler<MyRequest, MyResponse>(recordingEvents, MyRequest::class.java) {
        override fun handler(): Handler<MyRequest, MyResponse> = Handler { req, _ -> MyResponse(answer = req.message) }
    }

    @Test
    fun `can serialise inputs`() {
        val response = handler.handleRequest(mapOf("message" to "Hello"), TestContext())
        assertThat(response, equalTo(mapOf("answer" to "Hello")))
    }

    @Test
    fun `handles exceptions while deserializing`() {
        assertThatThrownBy { handler.handleRequest(mapOf(), TestContext()) }
            .isInstanceOf(RuntimeException::class.java)

        recordingEvents.contains(ExceptionThrown::class)
    }

    @Test
    fun `handles exceptions while processing`() {
        val handler = object : DirectHandler<MyRequest, MyResponse>(recordingEvents, MyRequest::class.java) {
            override fun handler(): Handler<MyRequest, MyResponse> = Handler { _, _ -> error("oh no!") }
        }
        assertThatThrownBy { handler.handleRequest(mapOf("message" to "hello"), TestContext()) }
            .isInstanceOf(RuntimeException::class.java)

        recordingEvents.contains(ExceptionThrown::class)
    }

    data class MyRequest(val message: String)
    data class MyResponse(val answer: String)
}
