package uk.nhs.nhsx.core.events

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.startsWith
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.handler.RequestContext
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.asJsonString
import uk.nhs.nhsx.testhelper.assertions.isSameAsJson
import java.time.Instant
import java.util.*

class PrintingJsonEventsTest {

    data class MyEvent(
        val field1: Int,
        val field2: Instant,
        val field3: UUID,
        val field4: DiagnosisKeySubmissionToken,
        val field5: Class<*>
    ) : Event(Info)

    data class MyEventWithMoreMetadata(val hello: String) : Event(Info, "label" to "greetings")

    @AfterEach
    fun teardown() {
        RequestContext.removeAwsRequestId()
    }

    @Test
    fun `prints throwable`() {
        RequestContext.assignAwsRequestId("12345")

        expectThat(ExceptionThrown(RuntimeException("foo")))
            .asJsonString
            .startsWith("""{"metadata":{"category":"ERROR","name":"ExceptionThrown","timestamp":"1970-01-01T00:00:00Z","awsRequestId":"12345"},"event":{"exception":"java.lang.RuntimeException: foo""")
    }

    @Test
    fun `prints an event with custom fields as JSON`() {
        RequestContext.assignAwsRequestId("12345")

        expectThat(
            MyEvent(
                123,
                Instant.EPOCH,
                UUID(0, 1),
                DiagnosisKeySubmissionToken.of("hello"),
                DiagnosisKeySubmissionToken::class.java
            )
        ).isSameAsJson(
            """
            {
              "metadata": {
                "category": "INFO",
                "name": "MyEvent",
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
            """
        )
    }

    @Test
    fun `appends the AWSRequestId from RequestContext`() {
        val context = TestContext().apply {
            requestId = UUID.fromString("d06971ab-21b4-4ebc-a951-a83c5a9b080e")
        }

        RequestContext.assignAwsRequestId(context.awsRequestId)

        expectThat(
            MyEvent(
                123,
                Instant.EPOCH,
                UUID(0, 1),
                DiagnosisKeySubmissionToken.of("hello"),
                DiagnosisKeySubmissionToken::class.java
            )
        ).isSameAsJson(
            """
            {
              "metadata": {
                "category": "INFO",
                "name": "MyEvent",
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
            """
        )
    }

    @Test
    fun `adds additional metadata`() {
        expectThat(MyEventWithMoreMetadata("hello")).isSameAsJson(
            """
             {
              "metadata": {
                "category": "INFO",
                "name": "MyEventWithMoreMetadata",
                "timestamp": "1970-01-01T00:00:00Z",
                "awsRequestId": "unknown",
                "label": "greetings"
              },
              "event": {
                "hello": "hello"
              }
            }
            """
        )
    }
}
