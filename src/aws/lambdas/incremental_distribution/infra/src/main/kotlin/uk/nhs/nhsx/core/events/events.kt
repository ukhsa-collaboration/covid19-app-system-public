package uk.nhs.nhsx.core.events

/**
 * Event interface for all structured logs
 */
abstract class Event(private val category: EventCategory) {
    fun category() = category
}

enum class EventCategory {
    Error, Warning, Info, Metric, Operational, Audit
}

interface Events : (Event) -> Unit
