@file:Suppress("BlockingMethodInNonBlockingContext")

package uk.nhs.nhsx.core.handler

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.OutputAssertions.hasSameContentAs
import uk.nhs.nhsx.testhelper.assertions.contains
import java.io.ByteArrayOutputStream
import java.util.*

class DirectHandlerTest {

    private val events = RecordingEvents()

    private val handler = object : DirectHandler<MyRequest, MyResponse>(events, MyRequest::class) {
        override fun handler(): Handler<MyRequest, MyResponse> = Handler { req, _ -> MyResponse(answer = req.message) }
    }

    @Test
    fun `can serialise inputs`() {
        val out = ByteArrayOutputStream()

        handler.handleRequest("""{"message":"hello"}""".byteInputStream(), out, TestContext())
        expectThat(out).hasSameContentAs("""{"answer":"hello"}""")
    }

    @Test
    fun `handles exceptions while deserializing`() {
        expectThrows<MismatchedInputException> {
            handler.handleRequest("""""".byteInputStream(), ByteArrayOutputStream(), TestContext())
        }

        expectThat(events).contains(ExceptionThrown::class)
    }

    @Test
    fun `handles exceptions while processing`() {
        val out = ByteArrayOutputStream()

        val handler = object : DirectHandler<MyRequest, MyResponse>(events, MyRequest::class) {
            override fun handler(): Handler<MyRequest, MyResponse> = Handler { _, _ -> error("oh no!") }
        }

        expectThrows<JsonParseException> {
            handler.handleRequest("""{"message";"hello"}""".byteInputStream(), out, TestContext())
        }

        expectThat(events).contains(ExceptionThrown::class)
    }

    @Test
    fun `assigns request id for logging to events`() {
        val uuid = UUID.randomUUID()

        val handler = object : DirectHandler<MyRequest, MyResponse>(events, MyRequest::class) {
            override fun handler(): Handler<MyRequest, MyResponse> = Handler { _, _ -> MyResponse("hello") }
        }

        handler.handleRequest(
            request = """{"message":"hello"}""".byteInputStream(),
            output = ByteArrayOutputStream(),
            context = TestContext().apply { requestId = uuid })

        expectThat(RequestContext.awsRequestId()).isEqualTo(uuid.toString())
    }

    data class MyRequest(val message: String)
    data class MyResponse(val answer: String)
}
