package uk.nhs.nhsx.core.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.startsWith
import org.apache.logging.log4j.ThreadContext
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken
import java.time.Instant
import java.util.*

class PrintingJsonEventsTest {

    data class MyGreatEvent(
        val field1: Int,
        val field2: Instant,
        val field3: UUID,
        val field4: DiagnosisKeySubmissionToken,
        val field5: Class<*>
    ) : Event(Info)

    @Test
    fun `prints throwable`() {
        val event = StringBuilder()
        val events = PrintingJsonEvents({ Instant.EPOCH }, { event.append(it) })
        events(String::class.java, ExceptionThrown(RuntimeException("foo")))

        assertThat(
            event.toString(),
            startsWith("""{"metadata":{"category":"ERROR","name":"ExceptionThrown","timestamp":"1970-01-01T00:00:00Z"},"event":{"exception":"java.lang.RuntimeException: foo""")
        )
    }

    @Test
    fun `prints an event with custom fields as JSON`() {
        val event = StringBuilder()
        val events = PrintingJsonEvents({ Instant.EPOCH }, { event.append(it) })
        events(String::class.java, MyGreatEvent(123, Instant.EPOCH, UUID(0, 1), DiagnosisKeySubmissionToken.of("hello"), DiagnosisKeySubmissionToken::class.java))

        JSONAssert.assertEquals("""
            {
              "metadata": {
                "category": "INFO",
                "name": "MyGreatEvent",
                "timestamp": "1970-01-01T00:00:00Z"
              },
              "event": {
                "field1": 123,
                "field2": "1970-01-01T00:00:00Z",
                "field3": "00000000-0000-0000-0000-000000000001",
                "field4": "hello",
                "field5": "DiagnosisKeySubmissionToken"
              }
            }
            """.trimIndent(), event.toString(), JSONCompareMode.STRICT)
    }

    @Test
    fun `appends the AWSRequestId from ThreadContext`() {
        try {
            ThreadContext.put("AWSRequestId", "d06971ab-21b4-4ebc-a951-a83c5a9b080e")

            val event = StringBuilder()
            val events = PrintingJsonEvents({ Instant.EPOCH }, { event.append(it) })
            events(String::class.java, MyGreatEvent(123, Instant.EPOCH, UUID(0, 1), DiagnosisKeySubmissionToken.of("hello"), DiagnosisKeySubmissionToken::class.java))

            JSONAssert.assertEquals("""
                {
                  "metadata": {
                    "category": "INFO",
                    "name": "MyGreatEvent",
                    "timestamp": "1970-01-01T00:00:00Z",
                    "awsRequestId":"d06971ab-21b4-4ebc-a951-a83c5a9b080e"
                  },
                  "event": {
                    "field1": 123,
                    "field2": "1970-01-01T00:00:00Z",
                    "field3": "00000000-0000-0000-0000-000000000001",
                    "field4": "hello",
                    "field5": "DiagnosisKeySubmissionToken"
                  }
                }
              """.trimIndent(), event.toString(), JSONCompareMode.STRICT)
        } finally {
            ThreadContext.remove("AWSRequestId")
        }
    }
}
