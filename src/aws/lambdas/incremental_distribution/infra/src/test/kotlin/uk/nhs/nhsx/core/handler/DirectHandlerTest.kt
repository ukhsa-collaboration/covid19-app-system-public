package uk.nhs.nhsx.core.handler

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class DirectHandlerTest {

    private val recordingEvents = RecordingEvents()

    private val handler = object : DirectHandler<MyRequest, MyResponse>(recordingEvents, MyRequest::class) {
        override fun handler(): Handler<MyRequest, MyResponse> = Handler { req, _ -> MyResponse(answer = req.message) }
    }

    @Test
    fun `can serialise inputs`() {
        val out = ByteArrayOutputStream()
        handler.handleRequest(
            """{"message":"hello"}""".byteInputStream(), out,
            TestContext()
        )
        assertThat(
            String(out.toByteArray()),
            equalTo("""{"answer":"hello"}""")
        )
    }

    @Test
    fun `handles exceptions while deserializing`() {
        assertThatThrownBy {
            handler.handleRequest(
                """""".byteInputStream(),
                ByteArrayOutputStream(),
                TestContext()
            )
        }
            .isInstanceOf(MismatchedInputException::class.java)

        recordingEvents.contains(ExceptionThrown::class)
    }

    @Test
    fun `handles exceptions while processing`() {
        val out = ByteArrayOutputStream()

        val handler = object : DirectHandler<MyRequest, MyResponse>(recordingEvents, MyRequest::class) {
            override fun handler(): Handler<MyRequest, MyResponse> = Handler { _, _ -> error("oh no!") }
        }

        assertThatThrownBy {
            handler.handleRequest(
                """{"message";"hello"}""".byteInputStream(),
                out,
                TestContext()
            )
        }
            .isInstanceOf(JsonParseException::class.java)

        recordingEvents.contains(ExceptionThrown::class)
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        val handler = object : DirectHandler<MyRequest, MyResponse>(recordingEvents, MyRequest::class) {
            override fun handler(): Handler<MyRequest, MyResponse> = Handler { _, _ -> MyResponse("hello") }
        }

        handler.handleRequest(
            """{"message":"hello"}""".byteInputStream(),
            ByteArrayOutputStream(),
            TestContext().apply {
                requestId = uuid
            }
        )

        assertThat(RequestContext.awsRequestId(), equalTo(uuid.toString()))
    }

    data class MyRequest(val message: String)
    data class MyResponse(val answer: String)
}
