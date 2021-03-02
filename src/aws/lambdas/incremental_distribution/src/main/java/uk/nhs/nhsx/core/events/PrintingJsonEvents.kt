package uk.nhs.nhsx.core.events

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.apache.logging.log4j.LogManager
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.core.ValueType
import uk.nhs.nhsx.core.events.EventCategory.*
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Supplier

class PrintingJsonEvents @JvmOverloads constructor(
    private val clock: Supplier<Instant>,
    private val print: (String) -> Unit = ::println
) : Events {

    override fun invoke(clazz: Class<*>, event: Event) {
        val asString = EVENTS_OBJECT_MAPPER.writeValueAsString(
            EventEnvelope(
                mapOf(
                    "category" to event.category(),
                    "name" to event.javaClass.simpleName,
                    "timestamp" to clock.get()
                ), event
            )
        )

        if (Error == event.category()) LogManager.getLogger(clazz::class.java).error(asString)
        if (Warning == event.category()) LogManager.getLogger(clazz::class.java).warn(asString)
        if (Info == event.category()) LogManager.getLogger(clazz::class.java).info(asString)

        print(asString)
    }
}

data class EventEnvelope(val metadata: Map<String, Any>, val event: Event) : Event(event.category())

private val EVENTS_OBJECT_MAPPER = SystemObjectMapper.objectMapper()
    .registerModule(SimpleModule().apply {
        add(DateTimeFormatter.ISO_INSTANT::format)
        add(Duration::toString)
        add(UUID::toString)
        add(Class<*>::getSimpleName)
        add { e: EventCategory -> e.name.toUpperCase() }
        add(ValueType<*>::value)
        add<Throwable> {
            StringWriter()
                .use { output ->
                    PrintWriter(output)
                        .use { printer ->
                            it.printStackTrace(printer);
                            output.toString()
                        }
                }
        }
    })

private inline fun <reified T> SimpleModule.add(crossinline fn: (T) -> String) {
    addSerializer(T::class.java, object : JsonSerializer<T>() {
        override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(fn(value))
        }
    })
}
