package uk.nhs.nhsx.circuitbreakerstats

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning

class EmptyCircuitBreakerAnalyticsLogs: Event(Info)
data class CircuitBreakerAnalyticsPollingFailed(val exception: Exception): Event(Warning)
