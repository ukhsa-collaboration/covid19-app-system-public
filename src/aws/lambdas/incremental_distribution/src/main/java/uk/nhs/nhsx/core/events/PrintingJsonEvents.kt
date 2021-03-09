package uk.nhs.nhsx.core.events

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.ThreadContext
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.events.EventCategory.Error
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning
import java.time.Instant
import java.util.function.Supplier

class PrintingJsonEvents @JvmOverloads constructor(
    private val clock: Supplier<Instant>,
    private val print: (String) -> Unit = ::println
) : Events {

    override fun invoke(clazz: Class<*>, event: Event) {
        val asString = Jackson.toJson(
            EventEnvelope(
                listOfNotNull(
                    "category" to event.category(),
                    "name" to event.javaClass.simpleName,
                    "timestamp" to clock.get(),

                    // TODO: set by aws-lambda-java-log4j2.. can we do this programmatically?
                    ThreadContext.get("AWSRequestId")?.let { "awsRequestId" to it }
                ).toMap(), event
            )
        )

        if (Error == event.category()) LogManager.getLogger(clazz::class.java).error(asString)
        if (Warning == event.category()) LogManager.getLogger(clazz::class.java).warn(asString)
        if (Info == event.category()) LogManager.getLogger(clazz::class.java).info(asString)

        print(asString)
    }
}

data class EventEnvelope(val metadata: Map<String, Any>, val event: Event) : Event(event.category())

