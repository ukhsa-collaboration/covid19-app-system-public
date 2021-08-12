package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info

object EmptyAnalyticsLogs : Event(Info)
object AnalyticsLogsPolling : Event(Info)
object AnalyticsLogsFinished : Event(Info)

data class QueryRequest(
    val queryStart: Long,
    val queryEnd: Long,
    val logGroup: String,
    val logInsightsQuery: String
) : Event(Info)
