package uk.nhs.nhsx.core.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.startsWith
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import uk.nhs.nhsx.core.handler.RequestContext
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.testhelper.ContextBuilder
import java.time.Instant
import java.util.UUID

class PrintingJsonEventsTest {

    data class MyGreatEvent(
        val field1: Int,
        val field2: Instant,
        val field3: UUID,
        val field4: DiagnosisKeySubmissionToken,
        val field5: Class<*>
    ) : Event(EventCategory.Info)

    @Test
    fun `prints throwable`() {
        RequestContext.assignAwsRequestId("12345")
        val event = StringBuilder()
        val events = PrintingJsonEvents({ Instant.EPOCH }, { event.append(it) })
        events(ExceptionThrown(RuntimeException("foo")))

        assertThat(
            event.toString(),
            startsWith("""{"metadata":{"category":"ERROR","name":"ExceptionThrown","timestamp":"1970-01-01T00:00:00Z","awsRequestId":"12345"},"event":{"exception":"java.lang.RuntimeException: foo""")
        )
    }

    @Test
    fun `prints an event with custom fields as JSON`() {
        RequestContext.assignAwsRequestId("12345")

        val event = StringBuilder()
        val events = PrintingJsonEvents({ Instant.EPOCH }, { event.append(it) })
        events(
            MyGreatEvent(
                123,
                Instant.EPOCH,
                UUID(0, 1),
                DiagnosisKeySubmissionToken.of("hello"),
                DiagnosisKeySubmissionToken::class.java
            )
        )

        assertEquals(
            """
            {
              "metadata": {
                "category": "INFO",
                "name": "MyGreatEvent",
                "timestamp": "1970-01-01T00:00:00Z",
                "awsRequestId": "12345"
              },
              "event": {
                "field1": 123,
                "field2": "1970-01-01T00:00:00Z",
                "field3": "00000000-0000-0000-0000-000000000001",
                "field4": "hello",
                "field5": "DiagnosisKeySubmissionToken"
              }
            }
            """.trimIndent(), event.toString(), STRICT
        )
    }

    @Test
    fun `appends the AWSRequestId from RequestContext`() {
        val s = ContextBuilder.TestContext()
            .apply { requestId = UUID.fromString("d06971ab-21b4-4ebc-a951-a83c5a9b080e") }
        RequestContext.assignAwsRequestId(s.awsRequestId)

        val event = StringBuilder()
        val events = PrintingJsonEvents({ Instant.EPOCH }, { event.append(it) })
        events(
            MyGreatEvent(
                123,
                Instant.EPOCH,
                UUID(0, 1),
                DiagnosisKeySubmissionToken.of("hello"),
                DiagnosisKeySubmissionToken::class.java
            )
        )

        assertEquals(
            """
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
              """.trimIndent(), event.toString(), STRICT
        )
    }
}
