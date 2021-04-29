package uk.nhs.nhsx.pubdash

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory

data class SendSqsMessageEvent(val queryId: QueryId, val dataset: Dataset) : Event(EventCategory.Info)
data class QueryFinishedEvent(val queryId: QueryId, val dataset: Dataset) : Event(EventCategory.Info)
data class QueryErrorEvent(val queryId: QueryId, val dataset: Dataset, val message: String) : Event(EventCategory.Error)
data class QueryStillRunning(val queryId: QueryId, val dataset: Dataset) : Event(EventCategory.Info)
