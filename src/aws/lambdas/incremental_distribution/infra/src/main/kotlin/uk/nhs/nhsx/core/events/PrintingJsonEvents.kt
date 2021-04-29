package uk.nhs.nhsx.core.events

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.handler.RequestContext

class PrintingJsonEvents @JvmOverloads constructor(
    private val clock: Clock,
    private val print: (String) -> Unit = ::println
) : Events {

    override fun invoke(event: Event) = print(
        Json.toJson(
            EventEnvelope(
                listOfNotNull<Pair<String, Any>>(
                    "category" to event.category(),
                    "name" to event.javaClass.simpleName,
                    "timestamp" to clock(),
                    "awsRequestId" to RequestContext.awsRequestId()
                ).toMap(), event
            )
        )
    )
}

data class EventEnvelope(val metadata: Map<String, Any>, val event: Event) : Event(event.category())

