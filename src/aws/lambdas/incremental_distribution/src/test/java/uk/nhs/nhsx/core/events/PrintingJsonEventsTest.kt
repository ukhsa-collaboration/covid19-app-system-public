package uk.nhs.nhsx.core.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.ValueType
import uk.nhs.nhsx.core.events.EventCategory.Info
import java.time.Instant
import java.util.UUID

class PrintingJsonEventsTest {

    class MyValueType(value: String) : ValueType<MyValueType>(value)
    data class MyGreatEvent(
        val field1: Int,
        val field2: Instant,
        val field3: UUID,
        val field4: MyValueType,
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
        events(String::class.java, MyGreatEvent(123, Instant.EPOCH, UUID(0, 1), MyValueType("hello"), MyValueType::class.java))

        assertThat(
            event.toString(),
            equalTo("""{"metadata":{"category":"INFO","name":"MyGreatEvent","timestamp":"1970-01-01T00:00:00Z"},"event":{"field1":123,"field2":"1970-01-01T00:00:00Z","field3":"00000000-0000-0000-0000-000000000001","field4":"hello","field5":"MyValueType"}}""")
        )
    }
}
