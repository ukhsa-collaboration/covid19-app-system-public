package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning

class EmptyAnalyticsLogs: Event(Info)

data class AnalyticsLogsPollingFailed(val exception: Exception): Event(Warning)
class AnalyticsLogsPolling: Event(Info)

data class QueryRequest(val queryStart: Long, val queryEnd: Long, val logGroup: String, val logInsightsQuery: String) : Event(Info)
