package uk.nhs.nhsx.core.queued

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import uk.nhs.nhsx.core.events.Event

interface Queued {
    fun interface Handler : (SQSEvent, Context) -> Event
}
