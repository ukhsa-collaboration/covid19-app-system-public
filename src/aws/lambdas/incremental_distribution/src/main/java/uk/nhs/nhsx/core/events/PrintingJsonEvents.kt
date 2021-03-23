package uk.nhs.nhsx.core.events

import org.apache.logging.log4j.ThreadContext
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Jackson

class PrintingJsonEvents @JvmOverloads constructor(
    private val clock: Clock,
    private val print: (String) -> Unit = ::println
) : Events {

    override fun invoke(event: Event) = print(Jackson.toJson(
        EventEnvelope(
            listOfNotNull<Pair<String, Any>>(
                "category" to event.category(),
                "name" to event.javaClass.simpleName,
                "timestamp" to clock(),

                // TODO: set by aws-lambda-java-log4j2.. can we do this programmatically?
                ThreadContext.get("AWSRequestId")?.let { "awsRequestId" to it }
            ).toMap(), event
        )
    ))
}

data class EventEnvelope(val metadata: Map<String, Any>, val event: Event) : Event(event.category())

